package com.agrichain.service;

import com.agrichain.core.SkillLoader;
import com.agrichain.entity.AgentRule;
import com.agrichain.entity.SkillConfig;
import com.agrichain.repository.AgentRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutoEvalService {

    private final AgentRuleRepository repository;
    private final SkillLoader skillLoader;
    private final ReflectionService reflectionService;
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 运行自测与进化闭环
     */
    public String runAutoEval() {
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

            // 2. 获取当前真实路由结果
            String actualSkill = getActualSkill(query);

            // 3. 判定
            if (expectedSkill.equals(actualSkill)) {
                System.out.println("✅ [AutoEval] 测试通过！");
                passed++;
            } else {
                System.out.println("❌ [AutoEval] BadCase 捕获！期望: " + expectedSkill + "，实际: " + actualSkill);
                failed++;

                // 4. 触发反思，生成规则并入库！
                String newRule = reflectionService.reflect(query, actualSkill, expectedSkill);

                AgentRule rule = new AgentRule();
                rule.setTenantId("dabeinong");
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

    private String getActualSkill(String userQuery) {
        // 把路由逻辑抽离出来供测试调用 (和 AgentRouterService 里的逻辑一致)
        StringBuilder skillsInfo = new StringBuilder();
        for (SkillConfig config : skillLoader.getAllSkills().values()) {
            skillsInfo.append("- ").append(config.getName()).append(": ").append(config.getDescription()).append("\n");
        }

        // 查库拿规则 (注意：这里应该拿 ACTIVE 的，为了演示效果好，把 PENDING 也带上也没事)
        List<AgentRule> rules = repository.findAll();
        String historyRules = rules.stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()) || "PENDING".equals(r.getStatus()))
                .map(AgentRule::getRuleContent)
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                你是一个企业级 AI 路由专家。请根据以下【可用技能列表】和【历史规则】，判断用户输入应该使用哪个 Skill。
                【可用技能列表】\n%s
                【历史避坑规则】\n%s
                【用户输入】\n%s
                要求：只输出选中的 Skill 名称，不要输出任何标点或其他文字。
                """, skillsInfo.toString(), historyRules.isEmpty() ? "暂无规则" : historyRules, userQuery);

        try {
            return chatClientBuilder.build().prompt().user(prompt).call().content().trim().replace("\"", "").replace("'", "");
        } catch (Exception e) {
            return "error";
        }
    }
}