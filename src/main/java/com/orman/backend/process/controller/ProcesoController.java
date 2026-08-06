package com.orman.backend.process.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.process.dto.request.CreateProcesoRequest;
import com.orman.backend.process.dto.request.UpdateProcesoRequest;
import com.orman.backend.process.dto.response.ProcesoResponse;
import com.orman.backend.process.service.ProcesoService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/procesos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class ProcesoController {
    private final ProcesoService procesoService;
    @PostMapping public ResponseEntity<ProcesoResponse> create(@Valid @RequestBody CreateProcesoRequest request) {
        ProcesoResponse response = procesoService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codp}").buildAndExpand(response.codp()).toUri();
        return ResponseEntity.created(location).body(response);
    }
    @GetMapping("/{codp}") public ProcesoResponse get(@PathVariable Integer codp) { return procesoService.get(codp); }
    @GetMapping public PageResponse<ProcesoResponse> list(@PageableDefault(page = 0, size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return procesoService.list(pageable.getPageSize() > 100 ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort()) : pageable);
    }
    @PutMapping("/{codp}") public ProcesoResponse update(@PathVariable Integer codp, @Valid @RequestBody UpdateProcesoRequest request) { return procesoService.update(codp, request); }
    @PatchMapping("/{codp}/activar") public ProcesoResponse activate(@PathVariable Integer codp) { return procesoService.activate(codp); }
    @PatchMapping("/{codp}/desactivar") public ProcesoResponse deactivate(@PathVariable Integer codp) { return procesoService.deactivate(codp); }
}
