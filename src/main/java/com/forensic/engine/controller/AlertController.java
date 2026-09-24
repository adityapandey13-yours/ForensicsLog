package com.forensic.engine.controller;

import com.forensic.engine.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);

    @Autowired
    private AlertService alertService;

    @GetMapping
    public ResponseEntity<?> getAlerts(@RequestParam(required = false) String severity,
                                       @RequestParam(required = false) String rule,
                                       @RequestParam(required = false) Long logFileId) {
        try {
            if (severity != null) {
                return ResponseEntity.ok(alertService.getAlertsBySeverity(severity));
            } else if (rule != null) {
                return ResponseEntity.ok(alertService.getAlertsByRule(rule));
            } else if (logFileId != null) {
                return ResponseEntity.ok(alertService.getAlertsByLogFile(logFileId));
            } else {
                return ResponseEntity.ok(alertService.getAllAlerts());
            }
        } catch (Exception e) {
            logger.error("Failed to fetch alerts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch alerts");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAlert(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(alertService.getAlertById(id));
        } catch (Exception e) {
            logger.error("Failed to fetch alert", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getAlertStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("countByRule", alertService.getAlertCountByRule());
            stats.put("countBySeverity", alertService.getAlertCountBySeverity());
            stats.put("countByCategory", alertService.getAlertCountByCategory());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to fetch alert stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch alert stats");
        }
    }
}
