package com.orman.backend.payment.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.PagoEstado;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface PagoService {

    PagoResponse create(Integer codcuo, PagoRequest request, MultipartFile comprobante, Authentication authentication);

    List<PagoResponse> listByCuota(Integer codcuo, Authentication authentication);

    PageResponse<PagoResponse> list(PagoEstado estado, MetodoPago metodo, Pageable pageable,
                                    Authentication authentication);

    PagoResponse get(Integer codpag, Authentication authentication);

    PagoResponse confirm(Integer codpag, Authentication authentication);

    PagoResponse reject(Integer codpag, PagoMotivoRequest request, Authentication authentication);

    PagoResponse annul(Integer codpag, PagoMotivoRequest request, Authentication authentication);
}
