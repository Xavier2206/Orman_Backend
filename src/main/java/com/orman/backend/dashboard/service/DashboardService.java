package com.orman.backend.dashboard.service;

import com.orman.backend.dashboard.dto.response.DashboardResumenFinancieroResponse;
import org.springframework.security.core.Authentication;

public interface DashboardService {

    DashboardResumenFinancieroResponse resumenFinanciero(Authentication authentication);
}
