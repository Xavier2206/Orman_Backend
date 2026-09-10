package com.orman.backend.person.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.personas")
public record PersonaPhotoProperties(@NotBlank String root,
                                    @Positive @Max(10 * 1024 * 1024) long maxFileSizeBytes,
                                    @Positive @Max(1024) int maxDimension) {
}
