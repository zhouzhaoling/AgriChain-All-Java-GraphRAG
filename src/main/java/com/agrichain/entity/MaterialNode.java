package com.agrichain.graph.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Data
@Node("Material")
public class MaterialNode {
    @Id
    private String name;
}