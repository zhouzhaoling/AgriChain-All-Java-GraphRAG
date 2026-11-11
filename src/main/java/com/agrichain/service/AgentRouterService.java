package com.agrichain.service;

import com.agrichain.core.SkillLoader;
import com.agrichain.entity.SkillConfig;
import com.agrichain.graph.repository.GraphQueryRepository; // 👈 引入图查询
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgentRouterService {

    private final GraphQueryRepository graphQueryRepository; // 👈 注入图数据库仓储
    private final SkillLoader skillLoader;
    private final SkillRoutingService skillRoutingService;

    public void routeAndExecute(String userQuery, String tenantId) {
        System.out.println("\n>>> [纯 Java Agent] 收到用户请求: " + userQuery);

        try {
            System.out.println(">>> [纯 Java Agent] 正在调用本地大模型进行意图路由...");
            String selectedSkillName = skillRoutingService.route(userQuery, tenantId, Set.of("ACTIVE"));

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
