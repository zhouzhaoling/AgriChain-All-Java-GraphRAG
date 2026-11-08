package com.agrichain.repository;

import com.agrichain.entity.AgentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRuleRepository extends JpaRepository<AgentRule, Long> {
}