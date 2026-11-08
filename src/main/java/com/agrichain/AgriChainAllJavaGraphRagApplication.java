package com.agrichain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
// 隔离 JPA 的扫描包
@EnableJpaRepositories(basePackages = "com.agrichain.repository")
// 隔离 Neo4j 的扫描包
@EnableNeo4jRepositories(basePackages = "com.agrichain.graph.repository")
public class AgriChainAllJavaGraphragApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgriChainAllJavaGraphragApplication.class, args);
    }
}