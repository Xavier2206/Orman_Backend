package com.orman.backend.payment.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.service.PagoService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PROPIETARIO')")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/cuotas/{codcuo}/pagos")
    public ResponseEntity<PagoResponse> create(@PathVariable Integer codcuo, @Valid @RequestBody PagoRequest request,
                                               Authentication authentication) {
        PagoResponse response = pagoService.create(codcuo, request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/pagos/{codpag}")
                .buildAndExpand(response.codpag()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/cuotas/{codcuo}/pagos")
    public java.util.List<PagoResponse> listByCuota(@PathVariable Integer codcuo, Authentication authentication) {
        return pagoService.listByCuota(codcuo, authentication);
    }

    @GetMapping("/pagos")
    public PageResponse<PagoResponse> list(@RequestParam(required = false)
                                           @Pattern(regexp = "PENDIENTE_REVISION|CONFIRMADO|RECHAZADO|ANULADO",
                                                   message = "El estado del Pago no es válido.") String estado,
                                           @RequestParam(required = false)
                                           @Pattern(regexp = "EFECTIVO|TRANSFERENCIA|QR",
                                                   message = "El método de Pago no es válido.") String metodo,
                                           @PageableDefault(page = 0, size = 20, sort = "fechaRegistro",
                                                   direction = Sort.Direction.DESC) Pageable pageable,
                                           Authentication authentication) {
        return pagoService.list(estado == null ? null : PagoEstado.valueOf(estado),
                metodo == null ? null : MetodoPago.valueOf(metodo), limit(pageable), authentication);
    }

    @GetMapping("/pagos/{codpag}")
    public PagoResponse get(@PathVariable Integer codpag, Authentication authentication) {
        return pagoService.get(codpag, authentication);
    }

    @PatchMapping("/pagos/{codpag}/confirmar")
    public PagoResponse confirm(@PathVariable Integer codpag, Authentication authentication) {
        return pagoService.confirm(codpag, authentication);
    }

    @PatchMapping("/pagos/{codpag}/rechazar")
    public PagoResponse reject(@PathVariable Integer codpag, @Valid @RequestBody PagoMotivoRequest request,
                               Authentication authentication) {
        return pagoService.reject(codpag, request, authentication);
    }

    @PatchMapping("/pagos/{codpag}/anular")
    public PagoResponse annul(@PathVariable Integer codpag, @Valid @RequestBody PagoMotivoRequest request,
                              Authentication authentication) {
        return pagoService.annul(codpag, request, authentication);
    }

    private Pageable limit(Pageable pageable) {
        return pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort()) : pageable;
    }
}
