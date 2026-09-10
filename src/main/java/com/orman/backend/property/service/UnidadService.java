package com.orman.backend.property.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.dto.response.UnidadResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface UnidadService {

    UnidadResponse create(Integer codprop, UnidadRequest request, Authentication authentication);

    PageResponse<UnidadResponse> listByPropiedad(Integer codprop, Pageable pageable, Authentication authentication);

    UnidadResponse get(Integer coduni, Authentication authentication);

    UnidadResponse update(Integer coduni, UnidadRequest request, Authentication authentication);
}
