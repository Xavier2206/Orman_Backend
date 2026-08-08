package com.orman.backend.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OtpResendRequest(@NotNull(message = "El challengeId es obligatorio.") UUID challengeId) {
}
