package com.orman.backend.property.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.unidades")
public record UnidadFotoProperties(@NotBlank String root,
                                  @Positive @Max(20 * 1024 * 1024) long maxFileSizeBytes,
                                  @Positive @Max(4096) int maxDimension) {
}
