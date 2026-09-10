package com.orman.backend.menu.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.menu.dto.request.CreateMenuRequest;
import com.orman.backend.menu.dto.request.UpdateMenuRequest;
import com.orman.backend.menu.dto.response.MenuResumenResponse;
import com.orman.backend.menu.dto.response.MenuResponse;
import org.springframework.data.domain.Pageable;

public interface MenuService {
    MenuResponse create(CreateMenuRequest request);
    MenuResponse get(Integer codm);
    PageResponse<MenuResponse> list(Pageable pageable);
    PageResponse<MenuResponse> list(String q, Short estado, Pageable pageable);
    MenuResumenResponse resumen();
    MenuResponse update(Integer codm, UpdateMenuRequest request);
    MenuResponse activate(Integer codm);
    MenuResponse deactivate(Integer codm);
}
