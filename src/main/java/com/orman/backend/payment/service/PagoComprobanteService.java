package com.orman.backend.payment.service;

import com.orman.backend.payment.dto.response.PagoComprobanteResponse;
import org.springframework.security.core.Authentication;

public interface PagoComprobanteService {

    PagoComprobanteResponse getMetadata(Integer codpag, Authentication authentication);

    PaymentImageContent getImage(Integer codpag, Authentication authentication);
}
