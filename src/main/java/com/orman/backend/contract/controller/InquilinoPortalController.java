package com.orman.backend.contract.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.contract.dto.response.InquilinoContratoResponse;
import com.orman.backend.contract.dto.response.InquilinoCuotaResponse;
import com.orman.backend.contract.service.InquilinoPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inquilino")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INQUILINO')")
public class InquilinoPortalController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InquilinoPortalService inquilinoPortalService;

    @GetMapping("/contratos")
    public PageResponse<InquilinoContratoResponse> listContracts(
            @PageableDefault(page = 0, size = 20) Pageable pageable, Authentication authentication) {
        return inquilinoPortalService.listContracts(boundedPageable(pageable), authentication);
    }

    @GetMapping("/contratos/{codcon}")
    public InquilinoContratoResponse getContract(@PathVariable Integer codcon, Authentication authentication) {
        return inquilinoPortalService.getContract(codcon, authentication);
    }

    @GetMapping("/contratos/{codcon}/cuotas")
    public List<InquilinoCuotaResponse> listQuotas(@PathVariable Integer codcon, Authentication authentication) {
        return inquilinoPortalService.listQuotas(codcon, authentication);
    }

    @GetMapping("/cuotas/{codcuo}")
    public InquilinoCuotaResponse getQuota(@PathVariable Integer codcuo, Authentication authentication) {
        return inquilinoPortalService.getQuota(codcuo, authentication);
    }

    private Pageable boundedPageable(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size);
    }
}
