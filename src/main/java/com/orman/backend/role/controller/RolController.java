package com.orman.backend.role.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.role.dto.response.RolResumenResponse;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.service.RolService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PROPIETARIO')")
public class RolController {

    private final RolService rolService;

    @GetMapping("/{codr}")
    public RolResponse get(@PathVariable Integer codr) {
        return rolService.get(codr);
    }

    @GetMapping("/resumen")
    public RolResumenResponse resumen() {
        return rolService.resumen();
    }

    @GetMapping
    public PageResponse<RolResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @Pattern(regexp = "0|1", message = "El estado debe ser 0 o 1.") String estado,
            @PageableDefault(page = 0, size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        Pageable limited = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;
        return rolService.list(q, estado == null ? null : Short.valueOf(estado), limited);
    }
}
