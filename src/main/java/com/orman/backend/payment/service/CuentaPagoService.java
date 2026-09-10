package com.orman.backend.payment.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.payment.dto.request.CuentaPagoRequest;
import com.orman.backend.payment.dto.response.CuentaPagoResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface CuentaPagoService {

    CuentaPagoResponse create(CuentaPagoRequest request, Authentication authentication);

    PageResponse<CuentaPagoResponse> list(Short estado, Pageable pageable, Authentication authentication);

    CuentaPagoResponse get(Integer codcta, Authentication authentication);

    CuentaPagoResponse update(Integer codcta, CuentaPagoRequest request, Authentication authentication);

    CuentaPagoResponse activate(Integer codcta, Authentication authentication);

    CuentaPagoResponse deactivate(Integer codcta, Authentication authentication);
}
