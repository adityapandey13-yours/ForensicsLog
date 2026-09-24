package com.forensic.engine.controller;

import com.forensic.engine.model.LogFile;
import com.forensic.engine.repository.LogFileRepository;
import com.forensic.engine.service.LogIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    @Autowired
    private LogIngestionService logIngestionService;

    @Autowired
    private LogFileRepository logFileRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadLog(@RequestParam("file") MultipartFile file) {
        try {
            LogFile logFile = logIngestionService.uploadAndProcess(file);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(logFile);
        } catch (Exception e) {
            logger.error("Upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllLogs() {
        try {
            return ResponseEntity.ok(logFileRepository.findAllByOrderByUploadedAtDesc());
        } catch (Exception e) {
            logger.error("Failed to fetch logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch logs");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLogFile(@PathVariable Long id) {
        try {
            Optional<LogFile> logFile = logFileRepository.findById(id);
            if (logFile.isPresent()) {
                return ResponseEntity.ok(logFile.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("LogFile not found");
            }
        } catch (Exception e) {
            logger.error("Failed to fetch log file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch log file");
        }
    }
}

