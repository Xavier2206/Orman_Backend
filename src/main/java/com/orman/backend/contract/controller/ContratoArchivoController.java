package com.orman.backend.contract.controller;

import com.orman.backend.contract.dto.request.ContratoArchivoRequest;
import com.orman.backend.contract.dto.response.ContratoArchivoResponse;
import com.orman.backend.contract.service.ContratoArchivoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/contratos/{codcon}/archivos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class ContratoArchivoController {

    private final ContratoArchivoService contratoArchivoService;

    @PostMapping
    public ResponseEntity<ContratoArchivoResponse> create(@PathVariable Integer codcon,
                                                           @Valid @RequestBody ContratoArchivoRequest request,
                                                           Authentication authentication) {
        ContratoArchivoResponse response = contratoArchivoService.create(codcon, request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<ContratoArchivoResponse> listByContrato(@PathVariable Integer codcon, Authentication authentication) {
        return contratoArchivoService.listByContrato(codcon, authentication);
    }
}
