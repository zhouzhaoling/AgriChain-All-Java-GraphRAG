package com.agrichain.repository;

import com.agrichain.entity.AgentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AgentRuleRepository extends JpaRepository<AgentRule, Long> {

    List<AgentRule> findByTenantIdAndStatusIn(String tenantId, Collection<String> statuses);
}