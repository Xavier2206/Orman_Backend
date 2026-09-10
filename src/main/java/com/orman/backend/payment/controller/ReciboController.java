package com.orman.backend.payment.controller;

import com.orman.backend.payment.dto.response.ReciboResponse;
import com.orman.backend.payment.service.ReciboService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class ReciboController {

    private final ReciboService reciboService;

    @GetMapping("/pagos/{codpag}/recibo")
    public ReciboResponse getByPago(@PathVariable Integer codpag, Authentication authentication) {
        return reciboService.getByPago(codpag, authentication);
    }

    @GetMapping("/recibos/{codrec}")
    public ReciboResponse get(@PathVariable Integer codrec, Authentication authentication) {
        return reciboService.get(codrec, authentication);
    }
}
