package com.orman.backend.payment.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.pagos")
public record PaymentImageStorageProperties(
        @NotBlank String root,
        @Positive @Max(5 * 1024 * 1024) long maxFileSizeBytes,
        @Positive @Max(1920) int maxDimension) {
}
