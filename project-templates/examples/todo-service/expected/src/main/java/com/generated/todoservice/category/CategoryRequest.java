package com.generated.todoservice.category;

import jakarta.validation.constraints.*;

public record CategoryRequest(
        @NotBlank String name
) {
}
