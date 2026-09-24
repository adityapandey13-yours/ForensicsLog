package com.forensic.engine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="log_files")
public class LogFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String filename;

    private Long fileSize;

    private Integer totalLines;

    private Integer alertCount = 0;

    @Column(nullable=false)
    private String status;

    private Long processingTimeMs;

    private LocalDateTime uploadedAt;

    private LocalDateTime completedAt;

    public LogFile() {}

    public LogFile(Long id, String filename, Long fileSize, Integer totalLines, Integer alertCount, String status, Long processingTimeMs, LocalDateTime uploadedAt, LocalDateTime completedAt) {
        this.id = id;
        this.filename = filename;
        this.fileSize = fileSize;
        this.totalLines = totalLines;
        this.alertCount = alertCount;
        this.status = status;
        this.processingTimeMs = processingTimeMs;
        this.uploadedAt = uploadedAt;
        this.completedAt = completedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "UPLOADING";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public Integer getTotalLines() { return totalLines; }
    public void setTotalLines(Integer totalLines) { this.totalLines = totalLines; }
    
    public Integer getAlertCount() { return alertCount; }
    public void setAlertCount(Integer alertCount) { this.alertCount = alertCount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
