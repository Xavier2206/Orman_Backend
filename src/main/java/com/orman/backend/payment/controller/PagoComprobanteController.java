package com.orman.backend.payment.controller;

import com.orman.backend.payment.dto.request.PagoComprobanteRequest;
import com.orman.backend.payment.dto.response.PagoComprobanteResponse;
import com.orman.backend.payment.service.PagoComprobanteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/pagos/{codpag}/comprobantes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class PagoComprobanteController {

    private final PagoComprobanteService pagoComprobanteService;

    @PostMapping
    public ResponseEntity<PagoComprobanteResponse> create(@PathVariable Integer codpag,
                                                           @Valid @RequestBody PagoComprobanteRequest request,
                                                           Authentication authentication) {
        PagoComprobanteResponse response = pagoComprobanteService.create(codpag, request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<PagoComprobanteResponse> list(@PathVariable Integer codpag, Authentication authentication) {
        return pagoComprobanteService.listByPago(codpag, authentication);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer codpag, @PathVariable Integer id,
                                       Authentication authentication) {
        pagoComprobanteService.delete(codpag, id, authentication);
        return ResponseEntity.noContent().build();
    }
}
