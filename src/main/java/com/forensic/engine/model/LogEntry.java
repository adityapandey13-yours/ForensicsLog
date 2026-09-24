package com.forensic.engine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name="log_entries", indexes={
    @Index(name="idx_log_entries_file_id", columnList="log_file_id"),
    @Index(name="idx_log_entries_timestamp", columnList="timestamp")
})
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long logFileId;

    private Integer lineNumber;

    private String timestamp;

    private String level;

    private String source;

    @Column(columnDefinition="TEXT")
    private String message;

    @Column(columnDefinition="TEXT")
    private String rawLine;

    public LogEntry() {}

    public LogEntry(Long id, Long logFileId, Integer lineNumber, String timestamp, String level, String source, String message, String rawLine) {
        this.id = id;
        this.logFileId = logFileId;
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
        this.level = level;
        this.source = source;
        this.message = message;
        this.rawLine = rawLine;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLogFileId() { return logFileId; }
    public void setLogFileId(Long logFileId) { this.logFileId = logFileId; }

    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
}

