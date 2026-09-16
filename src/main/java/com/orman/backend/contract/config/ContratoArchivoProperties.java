package com.orman.backend.contract.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.contratos")
public record ContratoArchivoProperties(@NotBlank String root,
                                        @Positive long maxFileSizeBytes,
                                        @Positive long targetOptimizedSizeBytes,
                                        @Positive long maxImagePixelsToOptimize) {
}
