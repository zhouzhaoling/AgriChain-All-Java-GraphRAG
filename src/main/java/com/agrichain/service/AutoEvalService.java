package com.agrichain.service;

import com.agrichain.entity.AgentRule;
import com.agrichain.repository.AgentRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AutoEvalService {

    private final AgentRuleRepository repository;
    private final ReflectionService reflectionService;
    private final SkillRoutingService skillRoutingService;

    /**
     * 运行自测与进化闭环。规则查询和反思入库使用同一个租户。
     */
    public String runAutoEval(String tenantId) {
        System.out.println("\n🚀 [AutoEval] 开始运行自动化测试巡检...");

        // 1. 内置测试集：Query -> 期望的 Skill
        Map<String, String> testCases = Map.of(
                "这批豆粕流向哪里", "traceability",
                "生猪不吃食应该吃什么药", "knowledge_graph", // 期望走知识图谱
                "帮我查一下销售报表", "sales_analysis"       // 这个 skill 我们根本没配，必定触发 BadCase
        );

        int passed = 0;
        int failed = 0;

        for (Map.Entry<String, String> testCase : testCases.entrySet()) {
            String query = testCase.getKey();
            String expectedSkill = testCase.getValue();

            System.out.println("\n👉 [AutoEval] 正在测试: " + query + " | 期望结果: " + expectedSkill);

            String actualSkill;
            try {
                actualSkill = skillRoutingService.route(query, tenantId, Set.of("ACTIVE", "PENDING"));
            } catch (SkillRoutingException e) {
                System.out.println("❌ [AutoEval] 路由失败: " + e.getMessage());
                failed++;
                continue;
            }

            if (expectedSkill.equals(actualSkill)) {
                System.out.println("✅ [AutoEval] 测试通过！");
                passed++;
            } else {
                System.out.println("❌ [AutoEval] BadCase 捕获！期望: " + expectedSkill + "，实际: " + actualSkill);
                failed++;

                String newRule = reflectionService.reflect(query, actualSkill, expectedSkill);

                AgentRule rule = new AgentRule();
                rule.setTenantId(tenantId);
                rule.setRuleContent(newRule);
                rule.setStatus("PENDING"); // 落库为待审批
                repository.save(rule);
                System.out.println("💾 [AutoEval] 反思规则已保存至数据库，等待人工审批！");
            }
        }

        String summary = String.format("巡检完成！通过: %d, 失败: %d。", passed, failed);
        System.out.println("\n🏁 [AutoEval] " + summary);
        return summary;
    }
}
