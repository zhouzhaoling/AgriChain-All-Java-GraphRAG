package com.agrichain.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "agent_rule")
public class AgentRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private String ruleContent;
    private String status;
}