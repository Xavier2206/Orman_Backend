package com.orman.backend.dashboard.controller;

import com.orman.backend.dashboard.dto.response.DashboardResumenFinancieroResponse;
import com.orman.backend.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumen-financiero")
    public DashboardResumenFinancieroResponse resumenFinanciero(Authentication authentication) {
        return dashboardService.resumenFinanciero(authentication);
    }
}
