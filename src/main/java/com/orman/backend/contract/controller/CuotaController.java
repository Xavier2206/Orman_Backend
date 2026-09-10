package com.orman.backend.contract.controller;

import com.orman.backend.contract.dto.response.CuotaResponse;
import com.orman.backend.contract.service.CuotaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contratos/{codcon}/cuotas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class CuotaController {

    private final CuotaService cuotaService;

    @GetMapping
    public List<CuotaResponse> listByContrato(@PathVariable Integer codcon, Authentication authentication) {
        return cuotaService.listByContrato(codcon, authentication);
    }
}
