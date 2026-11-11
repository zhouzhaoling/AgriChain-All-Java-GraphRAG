package com.agrichain.controller;

import com.agrichain.entity.AgentRule;
import com.agrichain.repository.AgentRuleRepository;
import com.agrichain.service.AgentRouterService;
import com.agrichain.service.AutoEvalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentRouterService agentRouterService;
    private final AutoEvalService autoEvalService;
    private final AgentRuleRepository ruleRepository;

    // 聊天主路由
    @GetMapping("/chat")
    public String chat(@RequestParam String query, @RequestParam String tenantId) {
        agentRouterService.routeAndExecute(query, tenantId);
        return "路由完成，请查看 IDEA 控制台日志！";
    }

    // 👇 新增：触发自测进化
    @GetMapping("/eval")
    public String runEval(@RequestParam String tenantId) {
        return autoEvalService.runAutoEval(tenantId);
    }

    // 👇 新增：一键审批所有 PENDING 规则（演示用）
    @PutMapping("/approve-all")
    public String approveAll() {
        List<AgentRule> rules = ruleRepository.findAll();
        for (AgentRule rule : rules) {
            if ("PENDING".equals(rule.getStatus())) {
                rule.setStatus("ACTIVE");
                ruleRepository.save(rule);
            }
        }
        return "所有 PENDING 规则已审批为 ACTIVE！";
    }
}