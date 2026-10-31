package com.example.spec;

import java.util.List;

/**
 * ArchitectAgent 출력 ({@code schemas/architecture.schema.json}).
 */
public record Architecture(List<EntitySpec> entities) {

    public record EntitySpec(
            String name,
            String label,
            String description,
            List<String> featureIds,
            List<FieldSpec> fields,
            List<RelationSpec> relations
    ) {
    }

    public record FieldSpec(String name, String label, FieldType type, boolean required, boolean unique) {
    }

    public record RelationSpec(RelationType type, String fieldName, String target, boolean required) {
    }

    public enum RelationType {
        MANY_TO_ONE
    }
}
