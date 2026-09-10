package com.orman.backend.payment.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.payment.dto.request.CuentaPagoRequest;
import com.orman.backend.payment.dto.response.CuentaPagoResponse;
import com.orman.backend.payment.service.CuentaPagoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/cuentas-pago")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PROPIETARIO')")
public class CuentaPagoController {

    private final CuentaPagoService cuentaPagoService;

    @PostMapping
    public ResponseEntity<CuentaPagoResponse> create(@Valid @RequestBody CuentaPagoRequest request,
                                                      Authentication authentication) {
        CuentaPagoResponse response = cuentaPagoService.create(request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codcta}")
                .buildAndExpand(response.codcta()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PageResponse<CuentaPagoResponse> list(@RequestParam(required = false)
                                                  @Pattern(regexp = "0|1", message = "El estado debe ser 0 o 1.")
                                                  String estado,
                                                  @PageableDefault(page = 0, size = 20, sort = "orden",
                                                          direction = Sort.Direction.ASC) Pageable pageable,
                                                  Authentication authentication) {
        return cuentaPagoService.list(estado == null ? null : Short.valueOf(estado), limit(pageable), authentication);
    }

    @GetMapping("/{codcta}")
    public CuentaPagoResponse get(@PathVariable Integer codcta, Authentication authentication) {
        return cuentaPagoService.get(codcta, authentication);
    }

    @PutMapping("/{codcta}")
    public CuentaPagoResponse update(@PathVariable Integer codcta, @Valid @RequestBody CuentaPagoRequest request,
                                     Authentication authentication) {
        return cuentaPagoService.update(codcta, request, authentication);
    }

    @PatchMapping("/{codcta}/activar")
    public CuentaPagoResponse activate(@PathVariable Integer codcta, Authentication authentication) {
        return cuentaPagoService.activate(codcta, authentication);
    }

    @PatchMapping("/{codcta}/desactivar")
    public CuentaPagoResponse deactivate(@PathVariable Integer codcta, Authentication authentication) {
        return cuentaPagoService.deactivate(codcta, authentication);
    }

    private Pageable limit(Pageable pageable) {
        return pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort()) : pageable;
    }
}
