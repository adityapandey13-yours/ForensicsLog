package com.forensic.engine.repository;

import com.forensic.engine.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByLogFileId(Long logFileId);
    List<Alert> findByRuleName(String ruleName);
    List<Alert> findBySeverity(String severity);
    List<Alert> findAllByOrderByDetectedAtDesc();
    
    @Query("SELECT a.ruleName, COUNT(a) FROM Alert a GROUP BY a.ruleName")
    List<Object[]> countByRuleName();
    
    @Query("SELECT a.severity, COUNT(a) FROM Alert a GROUP BY a.severity")
    List<Object[]> countBySeverity();
    
    @Query("SELECT a.category, COUNT(a) FROM Alert a GROUP BY a.category")
    List<Object[]> countByCategory();
    
    long countByLogFileId(Long logFileId);
}
