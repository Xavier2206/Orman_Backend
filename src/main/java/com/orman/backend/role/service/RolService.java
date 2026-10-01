package com.orman.backend.role.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.role.dto.response.RolResumenResponse;
import com.orman.backend.role.dto.response.RolResponse;
import org.springframework.data.domain.Pageable;

public interface RolService {


    RolResponse get(Integer codr);

    PageResponse<RolResponse> list(Pageable pageable);

    PageResponse<RolResponse> list(String q, Short estado, Pageable pageable);

    RolResumenResponse resumen();



}
