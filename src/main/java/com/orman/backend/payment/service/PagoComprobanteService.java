package com.orman.backend.payment.service;

import com.orman.backend.payment.dto.request.PagoComprobanteRequest;
import com.orman.backend.payment.dto.response.PagoComprobanteResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface PagoComprobanteService {

    PagoComprobanteResponse create(Integer codpag, PagoComprobanteRequest request, Authentication authentication);

    List<PagoComprobanteResponse> listByPago(Integer codpag, Authentication authentication);

    void delete(Integer codpag, Integer id, Authentication authentication);
}
