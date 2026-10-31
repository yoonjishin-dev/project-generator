package com.generated.todoservice.todo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodoResponse(
        Long id,
        String title,
        String memo,
        LocalDate dueDate,
        Boolean completed,
        Long categoryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TodoResponse from(Todo entity) {
        return new TodoResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getMemo(),
                entity.getDueDate(),
                entity.getCompleted(),
                entity.getCategory() != null ? entity.getCategory().getId() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
