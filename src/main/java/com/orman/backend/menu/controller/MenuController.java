package com.orman.backend.menu.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.menu.dto.request.CreateMenuRequest;
import com.orman.backend.menu.dto.request.UpdateMenuRequest;
import com.orman.backend.menu.dto.response.MenuResumenResponse;
import com.orman.backend.menu.dto.response.MenuResponse;
import com.orman.backend.menu.service.MenuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PROPIETARIO')")
public class MenuController {
    private final MenuService menuService;
    @PostMapping public ResponseEntity<MenuResponse> create(@Valid @RequestBody CreateMenuRequest request) {
        MenuResponse response = menuService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codm}").buildAndExpand(response.codm()).toUri();
        return ResponseEntity.created(location).body(response);
    }
    @GetMapping("/{codm}") public MenuResponse get(@PathVariable Integer codm) { return menuService.get(codm); }
    @GetMapping("/resumen") public MenuResumenResponse resumen() { return menuService.resumen(); }
    @GetMapping public PageResponse<MenuResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Pattern(regexp = "0|1", message = "El estado debe ser 0 o 1.") String estado,
            @PageableDefault(page = 0, size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        Pageable limited = pageable.getPageSize() > 100 ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort()) : pageable;
        return menuService.list(q, estado == null ? null : Short.valueOf(estado), limited);
    }
    @PutMapping("/{codm}") public MenuResponse update(@PathVariable Integer codm, @Valid @RequestBody UpdateMenuRequest request) { return menuService.update(codm, request); }
    @PatchMapping("/{codm}/activar") public MenuResponse activate(@PathVariable Integer codm) { return menuService.activate(codm); }
    @PatchMapping("/{codm}/desactivar") public MenuResponse deactivate(@PathVariable Integer codm) { return menuService.deactivate(codm); }
}
