package com.orman.backend.property.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.dto.response.UnidadResponse;
import com.orman.backend.property.service.UnidadService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class UnidadController {

    private final UnidadService unidadService;

    @PostMapping("/propiedades/{codprop}/unidades")
    public ResponseEntity<UnidadResponse> create(@PathVariable Integer codprop,
                                                  @Valid @RequestBody UnidadRequest request,
                                                  Authentication authentication) {
        UnidadResponse response = unidadService.create(codprop, request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/unidades/{coduni}")
                .buildAndExpand(response.coduni()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/propiedades/{codprop}/unidades")
    public PageResponse<UnidadResponse> listByPropiedad(@PathVariable Integer codprop,
                                                         @PageableDefault(page = 0, size = 20, sort = "nombre",
                                                                 direction = Sort.Direction.ASC) Pageable pageable,
                                                         Authentication authentication) {
        return unidadService.listByPropiedad(codprop, limit(pageable), authentication);
    }

    @GetMapping("/unidades/{coduni}")
    public UnidadResponse get(@PathVariable Integer coduni, Authentication authentication) {
        return unidadService.get(coduni, authentication);
    }

    @PutMapping("/unidades/{coduni}")
    public UnidadResponse update(@PathVariable Integer coduni, @Valid @RequestBody UnidadRequest request,
                                 Authentication authentication) {
        return unidadService.update(coduni, request, authentication);
    }

    private Pageable limit(Pageable pageable) {
        return pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;
    }
}
