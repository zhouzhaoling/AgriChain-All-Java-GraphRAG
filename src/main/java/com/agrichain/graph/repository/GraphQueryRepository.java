package com.agrichain.graph.repository;

import com.agrichain.graph.entity.MaterialNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GraphQueryRepository extends Neo4jRepository<MaterialNode, String> {

    // 核心 Cypher：多跳查询物料 -> 产品 -> 养殖场
    @Query("MATCH (m:Material {name: $materialName})-[:USED_IN]->(p:Product)-[:DELIVERED_TO]->(f:Farm) RETURN f.name")
    String findAffectedFarmByMaterial(String materialName);
}