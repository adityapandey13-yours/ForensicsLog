package com.forensic.engine.service;

import com.forensic.engine.model.Alert;
import com.forensic.engine.model.ForensicTimelineEvent;
import com.forensic.engine.model.LogEntry;
import com.forensic.engine.model.LogFile;
import com.forensic.engine.repository.AlertRepository;
import com.forensic.engine.repository.ForensicTimelineRepository;
import com.forensic.engine.repository.LogEntryRepository;
import com.forensic.engine.repository.LogFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(LogIngestionService.class);

    private static final Pattern LOG_PATTERN = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})\\]\\s*\\[(\\w+)\\]\\s*\\[([^\\]]+)\\]\\s*(.+)");
    private static final int BATCH_SIZE = 500;

    @Autowired
    private LogFileRepository logFileRepository;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private ForensicTimelineRepository forensicTimelineRepository;

    @Autowired
    private IocDetectionService iocDetectionService;

    @Autowired
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private LogIngestionService self;

    public LogFile uploadAndProcess(MultipartFile file) {
        try {
            Path tempDir = Files.createTempDirectory("forensic-engine");
            Path tempFile = tempDir.resolve(file.getOriginalFilename());
            file.transferTo(tempFile);

            LogFile logFile = new LogFile();
            logFile.setFilename(file.getOriginalFilename());
            logFile.setFileSize(file.getSize());
            logFile.setStatus("UPLOADING");
            logFile.setUploadedAt(LocalDateTime.now());
            logFile = logFileRepository.save(logFile);

            self.ingestFile(tempFile.toString(), logFile.getId());
            return logFile;
        } catch (IOException e) {
            logger.error("Failed to upload file", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Async
    public CompletableFuture<Long> ingestFile(String filePath, Long logFileId) {
        long startTime = System.currentTimeMillis();
        LogFile logFile = logFileRepository.findById(logFileId).orElseThrow(() -> new RuntimeException("LogFile not found"));
        logFile.setStatus("PROCESSING");
        logFileRepository.save(logFile);

        int totalLines = 0;
        int lineNumber = 0;
        List<LogEntry> entryBatch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                totalLines++;

                LogEntry entry = new LogEntry();
                entry.setLogFileId(logFileId);
                entry.setLineNumber(lineNumber);
                entry.setRawLine(line);

                Matcher matcher = LOG_PATTERN.matcher(line);
                if (matcher.find()) {
                    entry.setTimestamp(matcher.group(1));
                    entry.setLevel(matcher.group(2));
                    entry.setSource(matcher.group(3));
                    entry.setMessage(matcher.group(4));
                } else {
                    entry.setTimestamp("UNKNOWN");
                    entry.setLevel("UNKNOWN");
                    entry.setSource("UNKNOWN");
                    entry.setMessage("UNKNOWN");
                }

                entryBatch.add(entry);

                if (entryBatch.size() >= BATCH_SIZE) {
                    processBatch(entryBatch, logFileId);
                    entryBatch.clear();
                }
            }

            if (!entryBatch.isEmpty()) {
                processBatch(entryBatch, logFileId);
            }

            long processingTimeMs = System.currentTimeMillis() - startTime;
            Long alertCount = alertRepository.countByLogFileId(logFileId);

            logFile.setTotalLines(totalLines);
            logFile.setAlertCount(alertCount != null ? alertCount.intValue() : 0);
            logFile.setStatus("COMPLETED");
            logFile.setProcessingTimeMs(processingTimeMs);
            logFile.setCompletedAt(LocalDateTime.now());
            logFileRepository.save(logFile);

            logger.info("Completed processing file {}. Lines: {}, Alerts: {}, Time: {}ms", logFile.getFilename(), totalLines, alertCount, processingTimeMs);
            return CompletableFuture.completedFuture(alertCount != null ? alertCount : 0L);

        } catch (Exception e) {
            logger.error("Failed to process file {}", filePath, e);
            logFile.setStatus("FAILED");
            logFileRepository.save(logFile);
            return CompletableFuture.completedFuture(0L);
        }
    }

    @Transactional
    protected void processBatch(List<LogEntry> batch, Long logFileId) {
        List<LogEntry> savedEntries = logEntryRepository.saveAll(batch);
        
        List<CompletableFuture<List<Alert>>> alertFutures = new ArrayList<>();
        for (LogEntry entry : savedEntries) {
            CompletableFuture<List<Alert>> future = CompletableFuture.supplyAsync(() -> iocDetectionService.detectThreats(entry, logFileId), taskExecutor);
            alertFutures.add(future);
        }

        List<Alert> allAlerts = new ArrayList<>();
        for (CompletableFuture<List<Alert>> future : alertFutures) {
            try {
                allAlerts.addAll(future.get());
            } catch (Exception e) {
                logger.error("Error detecting threats for entry", e);
            }
        }

        alertRepository.saveAll(allAlerts);

        List<ForensicTimelineEvent> timelineEvents = new ArrayList<>();
        for (LogEntry entry : savedEntries) {
            if ("ERROR".equals(entry.getLevel()) || "WARN".equals(entry.getLevel())) {
                ForensicTimelineEvent event = new ForensicTimelineEvent();
                event.setLogFileId(logFileId);
                event.setEventType("LOG_LEVEL");
                event.setEventSource(entry.getSource());
                event.setEventDetail(entry.getMessage());
                event.setEventTimestamp(entry.getTimestamp());
                event.setSeverity(entry.getLevel());
                timelineEvents.add(event);
            }
        }

        for (Alert alert : allAlerts) {
            ForensicTimelineEvent event = new ForensicTimelineEvent();
            event.setLogFileId(logFileId);
            event.setEventType("ALERT");
            event.setEventSource(alert.getRuleName());
            event.setEventDetail("IOC Match: " + alert.getCategory());
            event.setEventTimestamp(alert.getTimestamp());
            event.setSeverity(alert.getSeverity());
            timelineEvents.add(event);
        }

        forensicTimelineRepository.saveAll(timelineEvents);
    }
}

