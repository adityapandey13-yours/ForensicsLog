package com.forensic.engine.dto;

public class LogFileResponse {
    private Long id;
    private String filename;
    private Long fileSize;
    private Integer totalLines;
    private Integer alertCount;
    private String status;
    private Long processingTimeMs;
    private String uploadedAt;
    private String completedAt;

    public LogFileResponse() {}

    public LogFileResponse(Long id, String filename, Long fileSize, Integer totalLines, Integer alertCount, String status, Long processingTimeMs, String uploadedAt, String completedAt) {
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

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
