package com.orman.backend.contract.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.contract.dto.request.CuotaListCriteria;
import com.orman.backend.contract.dto.response.CuotaListItemResponse;
import com.orman.backend.contract.service.CuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuotas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class CuotaListController {

    private final CuotaService cuotaService;

    @GetMapping
    public PageResponse<CuotaListItemResponse> list(
            @RequestParam(required = false) String codperInquilino,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String vencimiento,
            @RequestParam(required = false) String codprop,
            @RequestParam(required = false) String coduni,
            @RequestParam(required = false) String conPagoPendienteRevision,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            Authentication authentication) {
        CuotaListCriteria criteria = CuotaListCriteria.from(codperInquilino, periodo, estado, vencimiento,
                codprop, coduni, conPagoPendienteRevision, page, size);
        return cuotaService.listGlobal(criteria, authentication);
    }
}
