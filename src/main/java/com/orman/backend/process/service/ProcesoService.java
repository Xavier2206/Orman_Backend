package com.orman.backend.process.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.process.dto.request.CreateProcesoRequest;
import com.orman.backend.process.dto.request.UpdateProcesoRequest;
import com.orman.backend.process.dto.response.ProcesoResponse;
import org.springframework.data.domain.Pageable;

public interface ProcesoService {
    ProcesoResponse create(CreateProcesoRequest request);
    ProcesoResponse get(Integer codp);
    PageResponse<ProcesoResponse> list(Pageable pageable);
    ProcesoResponse update(Integer codp, UpdateProcesoRequest request);
    ProcesoResponse activate(Integer codp);
    ProcesoResponse deactivate(Integer codp);
}
