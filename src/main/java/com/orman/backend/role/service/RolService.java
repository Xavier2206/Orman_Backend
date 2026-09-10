package com.orman.backend.role.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.role.dto.request.CreateRolRequest;
import com.orman.backend.role.dto.request.UpdateRolRequest;
import com.orman.backend.role.dto.response.RolResumenResponse;
import com.orman.backend.role.dto.response.RolResponse;
import org.springframework.data.domain.Pageable;

public interface RolService {

    RolResponse create(CreateRolRequest request);

    RolResponse get(Integer codr);

    PageResponse<RolResponse> list(Pageable pageable);

    PageResponse<RolResponse> list(String q, Short estado, Pageable pageable);

    RolResumenResponse resumen();

    RolResponse update(Integer codr, UpdateRolRequest request);

    RolResponse activate(Integer codr);

    RolResponse deactivate(Integer codr);
}
