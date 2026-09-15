package com.orman.backend.contract.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.request.RescisionContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.entity.ContratoEstado;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface ContratoService {

    ContratoResponse create(Integer coduni, ContratoRequest request, Authentication authentication);

    PageResponse<ContratoResponse> listByUnidad(Integer coduni, Pageable pageable, Authentication authentication);

    PageResponse<ContratoResponse> list(Integer codprop, Integer coduni, ContratoEstado estado, Pageable pageable,
                                        Authentication authentication);

    ContratoResponse get(Integer codcon, Authentication authentication);

    ContratoResponse finish(Integer codcon, Authentication authentication);

    ContratoResponse rescind(Integer codcon, RescisionContratoRequest request, Authentication authentication);
}
