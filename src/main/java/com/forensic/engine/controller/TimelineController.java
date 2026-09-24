package com.forensic.engine.controller;

import com.forensic.engine.service.TimelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timeline")
public class TimelineController {
    private static final Logger logger = LoggerFactory.getLogger(TimelineController.class);

    @Autowired
    private TimelineService timelineService;

    @GetMapping
    public ResponseEntity<?> getTimeline() {
        try {
            return ResponseEntity.ok(timelineService.getAllTimelineEvents());
        } catch (Exception e) {
            logger.error("Failed to fetch timeline", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch timeline");
        }
    }

    @GetMapping("/{logFileId}")
    public ResponseEntity<?> getTimelineByFile(@PathVariable Long logFileId) {
        try {
            return ResponseEntity.ok(timelineService.getTimelineByLogFile(logFileId));
        } catch (Exception e) {
            logger.error("Failed to fetch timeline for logFileId " + logFileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch timeline");
        }
    }
}
