package com.orman.backend.property.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.property.dto.request.PropiedadRequest;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.dto.response.PropiedadResumenResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface PropiedadService {

    PropiedadResumenResponse resumen(Authentication authentication);

    PropiedadResponse create(PropiedadRequest request, Authentication authentication);

    PageResponse<PropiedadResponse> list(String q, String tipo, Short estado, Pageable pageable,
                                         Authentication authentication);

    PropiedadResponse get(Integer codprop, Authentication authentication);

    PropiedadResponse update(Integer codprop, PropiedadRequest request, Authentication authentication);

    PropiedadResponse activate(Integer codprop, Authentication authentication);

    PropiedadResponse deactivate(Integer codprop, Authentication authentication);
}
