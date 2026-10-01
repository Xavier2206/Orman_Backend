package com.orman.backend.push.dto;

import com.orman.backend.push.model.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushInstallationRequest(
        @NotBlank @Size(max = 128) String installationId,
        @NotNull PushPlatform platform) {
}
