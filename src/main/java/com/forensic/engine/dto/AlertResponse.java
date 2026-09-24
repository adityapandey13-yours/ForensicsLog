package com.forensic.engine.dto;

public class AlertResponse {
    private Long id;
    private String ruleName;
    private String severity;
    private String category;
    private Integer lineNumber;
    private String matchedLine;
    private String timestamp;
    private String detectedAt;
    private Long logFileId;

    public AlertResponse() {}

    public AlertResponse(Long id, String ruleName, String severity, String category, Integer lineNumber, String matchedLine, String timestamp, String detectedAt, Long logFileId) {
        this.id = id;
        this.ruleName = ruleName;
        this.severity = severity;
        this.category = category;
        this.lineNumber = lineNumber;
        this.matchedLine = matchedLine;
        this.timestamp = timestamp;
        this.detectedAt = detectedAt;
        this.logFileId = logFileId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }

    public String getMatchedLine() { return matchedLine; }
    public void setMatchedLine(String matchedLine) { this.matchedLine = matchedLine; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getDetectedAt() { return detectedAt; }
    public void setDetectedAt(String detectedAt) { this.detectedAt = detectedAt; }

    public Long getLogFileId() { return logFileId; }
    public void setLogFileId(Long logFileId) { this.logFileId = logFileId; }
}
