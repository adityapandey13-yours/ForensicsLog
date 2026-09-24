package com.forensic.engine.repository;

import com.forensic.engine.model.IocRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IocRuleRepository extends JpaRepository<IocRule, Long> {
    List<IocRule> findByEnabledTrue();
    Optional<IocRule> findByName(String name);
}
