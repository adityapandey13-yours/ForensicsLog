package com.forensic.engine.dto;

import java.util.List;
import java.util.Map;

public class StatsResponse {
    private long totalFiles;
    private long totalEntries;
    private long totalAlerts;
    private double avgProcessingTimeMs;
    private Map<String, Long> alertsBySeverity;
    private Map<String, Long> alertsByRule;
    private Map<String, Long> alertsByCategory;
    private List<LogFileResponse> recentFiles;

    public StatsResponse() {}

    public StatsResponse(long totalFiles, long totalEntries, long totalAlerts, double avgProcessingTimeMs, Map<String, Long> alertsBySeverity, Map<String, Long> alertsByRule, Map<String, Long> alertsByCategory, List<LogFileResponse> recentFiles) {
        this.totalFiles = totalFiles;
        this.totalEntries = totalEntries;
        this.totalAlerts = totalAlerts;
        this.avgProcessingTimeMs = avgProcessingTimeMs;
        this.alertsBySeverity = alertsBySeverity;
        this.alertsByRule = alertsByRule;
        this.alertsByCategory = alertsByCategory;
        this.recentFiles = recentFiles;
    }

    public long getTotalFiles() { return totalFiles; }
    public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }

    public long getTotalEntries() { return totalEntries; }
    public void setTotalEntries(long totalEntries) { this.totalEntries = totalEntries; }

    public long getTotalAlerts() { return totalAlerts; }
    public void setTotalAlerts(long totalAlerts) { this.totalAlerts = totalAlerts; }

    public double getAvgProcessingTimeMs() { return avgProcessingTimeMs; }
    public void setAvgProcessingTimeMs(double avgProcessingTimeMs) { this.avgProcessingTimeMs = avgProcessingTimeMs; }

    public Map<String, Long> getAlertsBySeverity() { return alertsBySeverity; }
    public void setAlertsBySeverity(Map<String, Long> alertsBySeverity) { this.alertsBySeverity = alertsBySeverity; }

    public Map<String, Long> getAlertsByRule() { return alertsByRule; }
    public void setAlertsByRule(Map<String, Long> alertsByRule) { this.alertsByRule = alertsByRule; }

    public Map<String, Long> getAlertsByCategory() { return alertsByCategory; }
    public void setAlertsByCategory(Map<String, Long> alertsByCategory) { this.alertsByCategory = alertsByCategory; }

    public List<LogFileResponse> getRecentFiles() { return recentFiles; }
    public void setRecentFiles(List<LogFileResponse> recentFiles) { this.recentFiles = recentFiles; }
}
