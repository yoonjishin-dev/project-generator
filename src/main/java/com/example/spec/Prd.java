package com.example.spec;

import java.util.List;

/**
 * PlannerAgent 출력 ({@code schemas/prd.schema.json}).
 */
public record Prd(
        String title,
        String summary,
        List<String> goals,
        List<TargetUser> targetUsers,
        List<Feature> features,
        List<String> nonFunctionalRequirements,
        List<String> outOfScope
) {

    public record TargetUser(String name, String description) {
    }

    public record Feature(String id, String name, String description, Priority priority, List<String> acceptanceCriteria) {
    }

    public enum Priority {
        MUST, SHOULD, COULD
    }
}
