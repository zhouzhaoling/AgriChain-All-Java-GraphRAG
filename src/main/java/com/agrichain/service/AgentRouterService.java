package com.agrichain.service;

import com.agrichain.core.SkillLoader;
import com.agrichain.entity.AgentRule;
import com.agrichain.entity.SkillConfig;
import com.agrichain.graph.repository.GraphQueryRepository; // 👈 引入图查询
import com.agrichain.repository.AgentRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentRouterService {

    private final AgentRuleRepository repository;
    private final GraphQueryRepository graphQueryRepository; // 👈 注入图数据库仓储
    private final SkillLoader skillLoader;
    private final ChatClient.Builder chatClientBuilder;

    public void routeAndExecute(String userQuery, String tenantId) {
        System.out.println("\n>>> [纯 Java Agent] 收到用户请求: " + userQuery);

        try {
            // 1. 准备技能描述 & 查库拿历史规则
            StringBuilder skillsInfo = new StringBuilder();
            for (SkillConfig config : skillLoader.getAllSkills().values()) {
                skillsInfo.append("- ").append(config.getName()).append(": ").append(config.getDescription()).append("\n");
            }

            List<AgentRule> activeRules = repository.findAll();
            String historyRules = activeRules.stream()
                    .filter(r -> "ACTIVE".equals(r.getStatus()))
                    .map(AgentRule::getRuleContent)
                    .collect(Collectors.joining("\n"));

            // 2. 调大模型路由
            String prompt = String.format("""
                    你是一个企业级 AI 路由专家。请根据以下【可用技能列表】和【历史规则】，判断用户输入应该使用哪个 Skill。
                    
                    【可用技能列表】
                    %s
                    
                    【历史避坑规则（必须严格遵守）】
                    %s
                    
                    【用户输入】
                    %s
                    
                    要求：只输出选中的 Skill 名称，不要输出任何标点、换行或其他文字。
                    """, skillsInfo.toString(), historyRules.isEmpty() ? "暂无规则" : historyRules, userQuery);

            System.out.println(">>> [纯 Java Agent] 正在调用本地大模型进行意图路由...");
            String selectedSkillName = chatClientBuilder.build().prompt()
                    .user(prompt).call().content().trim().replace("\"", "").replace("'", "");

            System.out.println(">>> [纯 Java Agent] 大模型决策出的 Skill: " + selectedSkillName);
            SkillConfig selectedSkill = skillLoader.getSkill(selectedSkillName);

            if (selectedSkill != null) {
                System.out.println("========== [纯 Java Agent] 即将执行技能: " + selectedSkillName + " ==========");

                // 3. 👇 新增：GraphAgent 执行层
                String graphResult = "";
                if ("traceability".equals(selectedSkillName)) {
                    // 在真实生产环境中，这里应该用大模型（如 Function Calling）提取实体。
                    // 为了 Demo 演示，我们简单用字符串匹配提取“豆粕”
                    if (userQuery.contains("豆粕")) {
                        System.out.println(">>> [GraphAgent] 检测到实体: [豆粕]，正在执行 Neo4j 多跳查询...");
                        graphResult = graphQueryRepository.findAffectedFarmByMaterial("豆粕");
                        System.out.println(">>> [GraphAgent] 图数据库返回结果: " + graphResult);
                    }
                }

                // 4. 拼装最终的终极 Prompt (包含图查询结果)
                String finalPrompt = String.format("""
                        %s
                        
                        【图数据库实时查询结果（请作为核心事实依据）】
                        %s
                        
                        请根据上述信息，回答用户的问题。
                        """, selectedSkill.getPromptBody(),
                        graphResult != null && !graphResult.isEmpty() ? "断供影响范围: " + graphResult : "未在图中找到相关链路");

                System.out.println("========== [最终发给大模型的 GraphRAG Prompt] ==========");
                System.out.println(finalPrompt);
                System.out.println("=========================================================");

            } else {
                System.out.println("❌ 找不到对应的 Skill 配置: " + selectedSkillName);
            }
        } catch (Exception e) {
            System.err.println("❌ [纯 Java Agent] 执行失败!");
            e.printStackTrace();
        }
    }
}