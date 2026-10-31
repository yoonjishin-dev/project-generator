package com.generated.todoservice.todo;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record TodoRequest(
        @NotBlank String title,
        String memo,
        LocalDate dueDate,
        @NotNull Boolean completed,
        Long categoryId
) {
}
