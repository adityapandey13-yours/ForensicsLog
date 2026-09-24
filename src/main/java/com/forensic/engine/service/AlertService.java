package com.forensic.engine.service;

import com.forensic.engine.dto.AlertResponse;
import com.forensic.engine.model.Alert;
import com.forensic.engine.repository.AlertRepository;
import com.forensic.engine.repository.LogFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlertService {
    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private LogFileRepository logFileRepository;

    public List<AlertResponse> getAllAlerts() {
        return alertRepository.findAllByOrderByDetectedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AlertResponse getAlertById(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found with id " + id));
        return mapToResponse(alert);
    }

    public List<AlertResponse> getAlertsByLogFile(Long logFileId) {
        return alertRepository.findByLogFileId(logFileId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AlertResponse> getAlertsBySeverity(String severity) {
        return alertRepository.findBySeverity(severity)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AlertResponse> getAlertsByRule(String ruleName) {
        return alertRepository.findByRuleName(ruleName)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getAlertCountByRule() {
        List<Object[]> results = alertRepository.countByRuleName();
        Map<String, Long> map = new HashMap<>();
        for (Object[] result : results) {
            map.put((String) result[0], (Long) result[1]);
        }
        return map;
    }

    public Map<String, Long> getAlertCountBySeverity() {
        List<Object[]> results = alertRepository.countBySeverity();
        Map<String, Long> map = new HashMap<>();
        for (Object[] result : results) {
            map.put((String) result[0], (Long) result[1]);
        }
        return map;
    }

    public Map<String, Long> getAlertCountByCategory() {
        List<Object[]> results = alertRepository.countByCategory();
        Map<String, Long> map = new HashMap<>();
        for (Object[] result : results) {
            map.put((String) result[0], (Long) result[1]);
        }
        return map;
    }

    private AlertResponse mapToResponse(Alert alert) {
        AlertResponse response = new AlertResponse();
        response.setId(alert.getId());
        response.setRuleName(alert.getRuleName());
        response.setSeverity(alert.getSeverity());
        response.setCategory(alert.getCategory());
        response.setLineNumber(alert.getLineNumber());
        response.setMatchedLine(alert.getMatchedLine());
        response.setTimestamp(alert.getTimestamp());
        response.setDetectedAt(alert.getDetectedAt() != null ? alert.getDetectedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        response.setLogFileId(alert.getLogFileId());
        return response;
    }
}

