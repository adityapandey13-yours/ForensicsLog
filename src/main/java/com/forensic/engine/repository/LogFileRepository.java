package com.forensic.engine.repository;

import com.forensic.engine.model.LogFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LogFileRepository extends JpaRepository<LogFile, Long> {
    List<LogFile> findAllByOrderByUploadedAtDesc();
}
