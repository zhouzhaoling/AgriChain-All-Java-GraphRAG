package com.agrichain.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ReflectionService {

    private final ChatClient chatClient;

    public ReflectionService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 纯 Java 反思提炼引擎
     */
    public String reflect(String userQuery, String wrongSkill, String correctSkill) {
        System.out.println("\n🧠 [Java 反思引擎] 发现 BadCase，正在触发大模型反思...");

        String prompt = String.format("""
            业务场景：用户输入 '%s'
            系统错误地使用了 %s，但专家指出应该使用 %s。
            请总结出一条简短的【避坑规则】，格式为：“当用户提到XXX时，务必路由到XXX”。
            只输出规则本身，不要标点。
            """, userQuery, wrongSkill, correctSkill);

        try {
            String rule = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim();
            System.out.println("✅ [Java 反思引擎] 规则提炼成功: " + rule);
            return rule;
        } catch (Exception e) {
            System.err.println("❌ [Java 反思引擎] 反思失败: " + e.getMessage());
            return "当用户提到未知错误时，请人工介入处理。"; // 兜底
        }
    }
}