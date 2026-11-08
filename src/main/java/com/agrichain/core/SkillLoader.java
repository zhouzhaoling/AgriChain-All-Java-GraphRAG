package com.agrichain.core;

import com.agrichain.entity.SkillConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class SkillLoader {

    private final Map<String, SkillConfig> skillCache = new HashMap<>();

    // 使用 @PostConstruct 在启动时自动加载
    @jakarta.annotation.PostConstruct
    public void init() {
        loadSkill("traceability.md");
        loadSkill("knowledge_graph.md");
        System.out.println("✅ [SkillLoader] 技能加载完成，共加载: " + skillCache.size() + " 个技能。");
    }

    private void loadSkill(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource("skills/" + fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }

            String[] parts = contentBuilder.toString().split("---");
            if (parts.length >= 3) {
                Yaml yaml = new Yaml();
                Map<String, Object> yamlMap = yaml.load(parts[1]);

                SkillConfig config = new SkillConfig();
                config.setName((String) yamlMap.get("name"));
                config.setDescription((String) yamlMap.get("description"));
                config.setPromptBody(parts[2].trim());

                skillCache.put(config.getName(), config);
            }
        } catch (Exception e) {
            System.err.println("❌ 加载技能失败: " + fileName);
        }
    }

    public SkillConfig getSkill(String name) {
        return skillCache.get(name);
    }

    public Map<String, SkillConfig> getAllSkills() {
        return skillCache;
    }
}