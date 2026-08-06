package com.orman.backend.process.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProcesoRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 60) String enlace) {
}
