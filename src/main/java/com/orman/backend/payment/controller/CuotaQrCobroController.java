package com.orman.backend.payment.controller;

import com.orman.backend.payment.dto.response.QrCobroResponse;
import com.orman.backend.payment.service.PaymentImageContent;
import com.orman.backend.payment.service.QrCobroService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuotas/{codcuo}/qr-cobro")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROPIETARIO','INQUILINO')")
public class CuotaQrCobroController {

    private final QrCobroService qrCobroService;

    @GetMapping
    public QrCobroResponse current(@PathVariable Integer codcuo, Authentication authentication) {
        return qrCobroService.currentForQuota(codcuo, authentication);
    }

    @GetMapping("/imagen")
    public ResponseEntity<Resource> image(@PathVariable Integer codcuo, Authentication authentication) {
        PaymentImageContent content = qrCobroService.imageForQuota(codcuo, authentication);
        return QrCobroController.imageResponse(content);
    }
}
