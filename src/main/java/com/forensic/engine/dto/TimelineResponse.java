package com.forensic.engine.dto;

public class TimelineResponse {
    private Long id;
    private String eventType;
    private String eventSource;
    private String eventDetail;
    private String eventTimestamp;
    private String severity;
    private Long logFileId;

    public TimelineResponse() {}

    public TimelineResponse(Long id, String eventType, String eventSource, String eventDetail, String eventTimestamp, String severity, Long logFileId) {
        this.id = id;
        this.eventType = eventType;
        this.eventSource = eventSource;
        this.eventDetail = eventDetail;
        this.eventTimestamp = eventTimestamp;
        this.severity = severity;
        this.logFileId = logFileId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventSource() { return eventSource; }
    public void setEventSource(String eventSource) { this.eventSource = eventSource; }

    public String getEventDetail() { return eventDetail; }
    public void setEventDetail(String eventDetail) { this.eventDetail = eventDetail; }

    public String getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(String eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Long getLogFileId() { return logFileId; }
    public void setLogFileId(Long logFileId) { this.logFileId = logFileId; }
}
