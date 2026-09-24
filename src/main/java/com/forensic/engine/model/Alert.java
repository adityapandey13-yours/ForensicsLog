package com.forensic.engine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="alerts", indexes={
    @Index(name="idx_alerts_file_id", columnList="log_file_id"),
    @Index(name="idx_alerts_rule_name", columnList="rule_name"),
    @Index(name="idx_alerts_severity", columnList="severity"),
    @Index(name="idx_alerts_detected_at", columnList="detected_at")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long logFileId;

    private Long logEntryId;

    private Long iocRuleId;

    private String ruleName;

    private String severity;

    private String category;

    private Integer lineNumber;

    @Column(columnDefinition="TEXT")
    private String matchedLine;

    private String timestamp;

    private LocalDateTime detectedAt;

    public Alert() {}

    public Alert(Long id, Long logFileId, Long logEntryId, Long iocRuleId, String ruleName, String severity, String category, Integer lineNumber, String matchedLine, String timestamp, LocalDateTime detectedAt) {
        this.id = id;
        this.logFileId = logFileId;
        this.logEntryId = logEntryId;
        this.iocRuleId = iocRuleId;
        this.ruleName = ruleName;
        this.severity = severity;
        this.category = category;
        this.lineNumber = lineNumber;
        this.matchedLine = matchedLine;
        this.timestamp = timestamp;
        this.detectedAt = detectedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.detectedAt == null) {
            this.detectedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLogFileId() { return logFileId; }
    public void setLogFileId(Long logFileId) { this.logFileId = logFileId; }

    public Long getLogEntryId() { return logEntryId; }
    public void setLogEntryId(Long logEntryId) { this.logEntryId = logEntryId; }

    public Long getIocRuleId() { return iocRuleId; }
    public void setIocRuleId(Long iocRuleId) { this.iocRuleId = iocRuleId; }

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

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
}

