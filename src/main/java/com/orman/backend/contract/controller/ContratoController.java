package com.orman.backend.contract.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.request.RescisionContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.service.ContratoService;
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
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping("/unidades/{coduni}/contratos")
    public ResponseEntity<ContratoResponse> create(@PathVariable Integer coduni,
                                                    @Valid @RequestBody ContratoRequest request,
                                                    Authentication authentication) {
        ContratoResponse response = contratoService.create(coduni, request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/contratos/{codcon}")
                .buildAndExpand(response.codcon()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/unidades/{coduni}/contratos")
    public PageResponse<ContratoResponse> listByUnidad(@PathVariable Integer coduni,
                                                        @PageableDefault(page = 0, size = 20, sort = "fechaInicio",
                                                                direction = Sort.Direction.DESC) Pageable pageable,
                                                        Authentication authentication) {
        return contratoService.listByUnidad(coduni, limit(pageable), authentication);
    }

    @GetMapping("/contratos")
    public PageResponse<ContratoResponse> list(@RequestParam(required = false) Integer coduni,
                                               @RequestParam(required = false) Integer codprop,
                                               @RequestParam(required = false) String q,
                                               @RequestParam(required = false)
                                               @Pattern(regexp = "PROGRAMADO|VIGENTE|FINALIZADO|RESCINDIDO",
                                                       message = "El estado del Contrato no es válido.") String estado,
                                               @PageableDefault(page = 0, size = 20, sort = "fechaInicio",
                                                       direction = Sort.Direction.DESC) Pageable pageable,
                                               Authentication authentication) {
        return contratoService.list(q, codprop, coduni,
                estado == null ? null : ContratoEstado.valueOf(estado), limit(pageable), authentication);
    }

    @GetMapping("/contratos/{codcon}")
    public ContratoResponse get(@PathVariable Integer codcon, Authentication authentication) {
        return contratoService.get(codcon, authentication);
    }

    @PatchMapping("/contratos/{codcon}/finalizar")
    public ContratoResponse finish(@PathVariable Integer codcon, Authentication authentication) {
        return contratoService.finish(codcon, authentication);
    }

    @PatchMapping("/contratos/{codcon}/rescindir")
    public ContratoResponse rescind(@PathVariable Integer codcon,
                                    @Valid @RequestBody RescisionContratoRequest request,
                                    Authentication authentication) {
        return contratoService.rescind(codcon, request, authentication);
    }

    private Pageable limit(Pageable pageable) {
        return pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;
    }
}
