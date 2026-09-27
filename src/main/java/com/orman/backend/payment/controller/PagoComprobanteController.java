package com.orman.backend.payment.controller;

import com.orman.backend.payment.dto.response.PagoComprobanteResponse;
import com.orman.backend.payment.service.PagoComprobanteService;
import com.orman.backend.payment.service.PaymentImageContent;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pagos/{codpag}/comprobante")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROPIETARIO','INQUILINO')")
public class PagoComprobanteController {

    private final PagoComprobanteService comprobanteService;

    @GetMapping("/metadata")
    public PagoComprobanteResponse metadata(@PathVariable Integer codpag, Authentication authentication) {
        return comprobanteService.getMetadata(codpag, authentication);
    }

    @GetMapping
    public ResponseEntity<Resource> image(@PathVariable Integer codpag, Authentication authentication) {
        PaymentImageContent content = comprobanteService.getImage(codpag, authentication);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(content.nombreArchivo(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.tipoContenido()))
                .contentLength(content.tamano())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().cachePrivate().getHeaderValue())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }
}
