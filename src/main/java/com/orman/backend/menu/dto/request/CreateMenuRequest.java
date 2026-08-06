package com.orman.backend.menu.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMenuRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 50) String icono,
        @Min(0) @Max(1) Short estado) {
}
