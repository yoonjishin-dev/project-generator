package com.example.spec;

import java.util.List;

/**
 * ApiAgent 출력 ({@code schemas/api.schema.json}).
 */
public record ApiSpec(List<ResourceSpec> resources) {

    public record ResourceSpec(String entity, String path, List<Operation> operations) {
    }

    public enum Operation {
        LIST, GET, CREATE, UPDATE, DELETE
    }
}
