package com.orman.backend.payment.service;

import com.orman.backend.payment.dto.response.ReciboResponse;
import org.springframework.security.core.Authentication;

public interface ReciboService {

    ReciboResponse getByPago(Integer codpag, Authentication authentication);

    ReciboResponse get(Integer codrec, Authentication authentication);
}
