package com.orman.backend.menu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMenuRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 50) String icono) {
}
