# 🚀 AgriChain-All-Java-GraphRAG (企业级纯 Java AI Agent 与自进化引擎)

本项目是一个面向农牧产业链的**企业级 AI 智能决策平台原型**。核心目标是展示如何在不依赖任何 Python 环境的情况下，**纯用 Java (Spring Boot 3 + Spring AI)** 落地具备 **GraphRAG (图检索增强生成)** 与 **AI Loop (自我反思与进化)** 能力的高级 Agent 系统。

## 🏗️ 核心架构与技术难点突破

本项目彻底摒弃了跨语言调用，实现了所有 AI 逻辑的 Java 内聚：

1. **纯 Java AI 路由与 MD 编排**：
    * 利用 Spring AI 直连本地 Ollama (Qwen2.5)，替代 Python LangChain。
    * 启动时动态解析 `resources/skills/*.md`，将 YAML 头部与 Markdown Prompt 分离，实现热拔插的技能编排。
2. **GraphAgent 多跳推理 (GraphRAG)**：
    * **核心痛点解决**：成功解决 JPA (H2) 与 Spring Data Neo4j 的 **双数据源事务管理器冲突**。
    * Java 直接驱动 Cypher 多跳查询（原料 -> 产品 -> 养殖场），将图谱的强关联事实注入到 Prompt 中，让大模型回答有真实依据。
3. **AI Loop 自循环进化引擎 (AutoEval)**：
    * 纯 Java 实现自动化测试集巡检。如果大模型路由错误（BadCase），自动触发 **反思引擎** 让大模型生成“避坑规则”并落库（PENDING）。
    * 规则审批（ACTIVE）后，动态拼接入下一轮 System Prompt，实现**系统越跑越聪明**，无需修改一行 Java 代码。

## 🛠️ 技术栈

| 模块 | 技术选型 | 核心职责 |
| :--- | :--- | :--- |
| **核心框架** | Spring Boot 3.4, Java 21 | 业务流转与主控 |
| **AI 引擎** | Spring AI, Ollama (Qwen2.5-7B) | 意图识别、规则反思提炼 |
| **存储引擎** | H2 (关系型), Neo4j (图数据库) | 规则状态机落库、产业链图谱多跳查询 |

## 🚀 快速启动

### 1. 依赖组件启动
```bash
# 启动 Neo4j 图数据库 (密码: password123)
docker run -p 7474:7474 -p 7687:7687 -v neo4j_data:/data -e NEO4J_AUTH=neo4j/password123 -d neo4j:5.26-community

# 初始化图数据 (在 Neo4j Browser 执行)
CREATE (s:Supplier {name: 'A原料集团'})-[:SUPPLIES]->(m:Material {name: '豆粕'})-[:USED_IN]->(p:Product {name: '仔猪饲料'})-[:DELIVERED_TO]->(f:Farm {name: 'B养殖场'})

# 启动本地 Ollama
ollama run qwen2.5:7b
```

### 2. 启动 Java 主干
在 IDEA 中运行 `AgriChainAllJavaGraphragApplication.java` (端口 `8081`)。

## 🎬 核心场景演示

1. **Agent 路由与 GraphRAG 推理**：
   `GET http://localhost:8081/api/agent/chat?query=这批豆粕流向哪里&tenantId=dabeinong`
   *现象*：控制台打印大模型决策出 `traceability`，随后执行 Cypher 查出 `B养殖场`，并拼装为终极 Prompt。

2. **触发 AI 自我进化巡检**：
   `GET http://localhost:8081/api/agent/eval`
   *现象*：系统发现“查销售报表”用例发生 BadCase，自动触发反思引擎，提炼规则落库。
   *(第一轮结果：通过 2，失败 1)*

3. **审批生效与二次巡检**：
   `PUT http://localhost:8081/api/agent/approve-all`
   再次请求 `/eval`。
   *现象*：因为规则已回注，第二次巡检全部通过。*(结果：通过 3，失败 0)*

## 🎤 架构思考与经验
* **彻底消除跨语言障碍**：对比于混合架构，纯 Java 版本在部署、事务管理和高并发处理上极其丝滑，是生产环境的首选。
* **多数据源冲突**：双数据库共存时，必须手动声明 `Neo4jTransactionManager` 与 `JpaTransactionManager`，并标记 `@Primary` 以解决自动配置引发的抢占问题。