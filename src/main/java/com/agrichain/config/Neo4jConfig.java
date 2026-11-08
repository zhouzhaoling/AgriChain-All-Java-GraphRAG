package com.agrichain.config;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class Neo4jConfig {

    // 1. 显式定义 Neo4j 的事务管理器
    @Bean("neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver, DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }

    // 2. 显式定义 Neo4jTemplate，并强制传入上面定义的 neo4jTransactionManager
    @Bean("neo4jTemplate")
    public Neo4jTemplate neo4jTemplate(Neo4jClient neo4jClient,
                                       Neo4jMappingContext neo4jMappingContext,
                                       Neo4jTransactionManager neo4jTransactionManager) {
        return new Neo4jTemplate(neo4jClient, neo4jMappingContext, neo4jTransactionManager);
    }

    // 3. 👇 核心修复：显式定义 JPA 的事务管理器，并标记为首选 (@Primary)
    @Bean("transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}