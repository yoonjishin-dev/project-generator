package com.generated.todoservice.category;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CategoryResponse from(Category entity) {
        return new CategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
