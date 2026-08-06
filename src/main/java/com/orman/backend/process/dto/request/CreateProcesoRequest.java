package com.orman.backend.process.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProcesoRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 60) String enlace,
        @Min(0) @Max(1) Short estado) {
}
