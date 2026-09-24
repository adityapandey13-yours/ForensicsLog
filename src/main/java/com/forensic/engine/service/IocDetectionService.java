package com.forensic.engine.service;

import com.forensic.engine.model.Alert;
import com.forensic.engine.model.IocRule;
import com.forensic.engine.model.LogEntry;
import com.forensic.engine.repository.IocRuleRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IocDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(IocDetectionService.class);

    @Autowired
    private IocRuleRepository iocRuleRepository;

    private List<CompiledRule> compiledRules = new ArrayList<>();

    private static class CompiledRule {
        Long id;
        String name;
        Pattern pattern;
        String severity;
        String category;

        CompiledRule(Long id, String name, Pattern pattern, String severity, String category) {
            this.id = id;
            this.name = name;
            this.pattern = pattern;
            this.severity = severity;
            this.category = category;
        }
    }

    @PostConstruct
    public void loadRules() {
        List<IocRule> rules = iocRuleRepository.findByEnabledTrue();
        for (IocRule rule : rules) {
            Pattern pattern = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE);
            compiledRules.add(new CompiledRule(rule.getId(), rule.getName(), pattern, rule.getSeverity(), rule.getCategory()));
        }
        logger.info("Loaded {} enabled IOC rules", compiledRules.size());
    }

    public List<Alert> detectThreats(LogEntry entry, Long logFileId) {
        List<Alert> alerts = new ArrayList<>();
        if (entry.getRawLine() == null) {
            return alerts;
        }
        for (CompiledRule rule : compiledRules) {
            Matcher matcher = rule.pattern.matcher(entry.getRawLine());
            if (matcher.find()) {
                Alert alert = new Alert();
                alert.setLogFileId(logFileId);
                alert.setLogEntryId(entry.getId());
                alert.setIocRuleId(rule.id);
                alert.setRuleName(rule.name);
                alert.setSeverity(rule.severity);
                alert.setCategory(rule.category);
                alert.setLineNumber(entry.getLineNumber());
                alert.setMatchedLine(entry.getRawLine());
                alert.setTimestamp(entry.getTimestamp());
                alert.setDetectedAt(LocalDateTime.now());
                alerts.add(alert);
            }
        }
        return alerts;
    }
}

