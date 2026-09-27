package com.orman.backend.payment.dto.request;

import com.orman.backend.payment.entity.QrCobroEstado;
import jakarta.validation.constraints.NotNull;

public record QrCobroEstadoRequest(@NotNull QrCobroEstado estado) {
}
