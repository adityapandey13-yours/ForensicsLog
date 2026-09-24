package com.forensic.engine.repository;

import com.forensic.engine.model.ForensicTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ForensicTimelineRepository extends JpaRepository<ForensicTimelineEvent, Long> {
    List<ForensicTimelineEvent> findByLogFileIdOrderByEventTimestampAsc(Long logFileId);
    List<ForensicTimelineEvent> findAllByOrderByEventTimestampAsc();
}
