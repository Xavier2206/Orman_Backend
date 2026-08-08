package com.orman.backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OtpVerifyRequest(
        @NotNull(message = "El challengeId es obligatorio.") UUID challengeId,
        @NotBlank(message = "El código OTP es obligatorio.")
        @Pattern(regexp = "\\d{6}", message = "El código OTP debe contener exactamente seis dígitos.") String code,
        @NotBlank(message = "El deviceId es obligatorio.") @Size(max = 100) String deviceId,
        @NotBlank(message = "El deviceName es obligatorio.") @Size(max = 100) String deviceName) {

    @Override public String toString() {
        return "OtpVerifyRequest[challengeId=" + challengeId + ", code=<redacted>, deviceId=" + deviceId
                + ", deviceName=" + deviceName + "]";
    }
}
