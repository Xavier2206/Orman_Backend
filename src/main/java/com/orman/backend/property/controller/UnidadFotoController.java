package com.orman.backend.property.controller;

import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.service.UnidadFotoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/unidades/{coduni}/fotos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class UnidadFotoController {

    private final UnidadFotoService unidadFotoService;

    @PostMapping
    public ResponseEntity<UnidadFotoResponse> create(@PathVariable Integer coduni,
                                                      @Valid @RequestBody UnidadFotoRequest request,
                                                      Authentication authentication) {
        UnidadFotoResponse response = unidadFotoService.create(coduni, request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<UnidadFotoResponse> listByUnidad(@PathVariable Integer coduni, Authentication authentication) {
        return unidadFotoService.listByUnidad(coduni, authentication);
    }

    @PutMapping("/{id}")
    public UnidadFotoResponse update(@PathVariable Integer coduni, @PathVariable Integer id,
                                     @Valid @RequestBody UnidadFotoRequest request,
                                     Authentication authentication) {
        return unidadFotoService.update(coduni, id, request, authentication);
    }

    @PatchMapping("/{id}/portada")
    public UnidadFotoResponse setPortada(@PathVariable Integer coduni, @PathVariable Integer id,
                                         Authentication authentication) {
        return unidadFotoService.setPortada(coduni, id, authentication);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer coduni, @PathVariable Integer id,
                                       Authentication authentication) {
        unidadFotoService.delete(coduni, id, authentication);
        return ResponseEntity.noContent().build();
    }
}
