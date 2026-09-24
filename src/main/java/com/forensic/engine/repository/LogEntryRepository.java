package com.forensic.engine.repository;

import com.forensic.engine.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByLogFileId(Long logFileId);
    long countByLogFileId(Long logFileId);
}
