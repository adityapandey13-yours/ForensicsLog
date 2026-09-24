package com.forensic.engine.service;

import com.forensic.engine.dto.LogFileResponse;
import com.forensic.engine.dto.StatsResponse;
import com.forensic.engine.model.LogFile;
import com.forensic.engine.repository.AlertRepository;
import com.forensic.engine.repository.LogEntryRepository;
import com.forensic.engine.repository.LogFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatsService {
    @Autowired
    private LogFileRepository logFileRepository;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    public StatsResponse getSystemStats() {
        StatsResponse stats = new StatsResponse();
        stats.setTotalFiles(logFileRepository.count());
        stats.setTotalEntries(logEntryRepository.count());
        stats.setTotalAlerts(alertRepository.count());

        List<LogFile> allFiles = logFileRepository.findAll();
        double avgTime = allFiles.stream()
                .filter(f -> "COMPLETED".equals(f.getStatus()) && f.getProcessingTimeMs() != null)
                .mapToLong(LogFile::getProcessingTimeMs)
                .average()
                .orElse(0.0);
        stats.setAvgProcessingTimeMs(avgTime);

        stats.setAlertsBySeverity(alertService.getAlertCountBySeverity());
        stats.setAlertsByRule(alertService.getAlertCountByRule());
        stats.setAlertsByCategory(alertService.getAlertCountByCategory());

        List<LogFileResponse> recent = logFileRepository.findAllByOrderByUploadedAtDesc()
                .stream()
                .limit(10)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        stats.setRecentFiles(recent);

        return stats;
    }

    private LogFileResponse mapToResponse(LogFile file) {
        LogFileResponse response = new LogFileResponse();
        response.setId(file.getId());
        response.setFilename(file.getFilename());
        response.setFileSize(file.getFileSize());
        response.setTotalLines(file.getTotalLines());
        response.setAlertCount(file.getAlertCount());
        response.setStatus(file.getStatus());
        response.setProcessingTimeMs(file.getProcessingTimeMs());
        response.setUploadedAt(file.getUploadedAt() != null ? file.getUploadedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        response.setCompletedAt(file.getCompletedAt() != null ? file.getCompletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return response;
    }
}

