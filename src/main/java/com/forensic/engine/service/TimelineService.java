package com.forensic.engine.service;

import com.forensic.engine.dto.TimelineResponse;
import com.forensic.engine.model.ForensicTimelineEvent;
import com.forensic.engine.repository.ForensicTimelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimelineService {
    @Autowired
    private ForensicTimelineRepository forensicTimelineRepository;

    public List<TimelineResponse> getAllTimelineEvents() {
        return forensicTimelineRepository.findAllByOrderByEventTimestampAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TimelineResponse> getTimelineByLogFile(Long logFileId) {
        return forensicTimelineRepository.findByLogFileIdOrderByEventTimestampAsc(logFileId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TimelineResponse mapToResponse(ForensicTimelineEvent event) {
        TimelineResponse response = new TimelineResponse();
        response.setId(event.getId());
        response.setEventType(event.getEventType());
        response.setEventSource(event.getEventSource());
        response.setEventDetail(event.getEventDetail());
        response.setEventTimestamp(event.getEventTimestamp());
        response.setSeverity(event.getSeverity());
        response.setLogFileId(event.getLogFileId());
        return response;
    }
}

