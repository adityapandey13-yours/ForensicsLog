package com.forensic.engine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name="forensic_timeline", indexes={
    @Index(name="idx_timeline_file_id", columnList="log_file_id"),
    @Index(name="idx_timeline_timestamp", columnList="event_timestamp")
})
public class ForensicTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long logFileId;

    private String eventType;

    private String eventSource;

    @Column(columnDefinition="TEXT")
    private String eventDetail;

    private String eventTimestamp;

    private String severity;

    public ForensicTimelineEvent() {}

    public ForensicTimelineEvent(Long id, Long logFileId, String eventType, String eventSource, String eventDetail, String eventTimestamp, String severity) {
        this.id = id;
        this.logFileId = logFileId;
        this.eventType = eventType;
        this.eventSource = eventSource;
        this.eventDetail = eventDetail;
        this.eventTimestamp = eventTimestamp;
        this.severity = severity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLogFileId() { return logFileId; }
    public void setLogFileId(Long logFileId) { this.logFileId = logFileId; }

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
}

