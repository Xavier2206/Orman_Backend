package com.orman.backend.property.controller;

import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.request.UnidadFotoMetadataRequest;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.service.UnidadFotoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/unidades/{coduni}/fotos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PROPIETARIO')")
public class UnidadFotoController {

    private final UnidadFotoService unidadFotoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UnidadFotoResponse> create(@PathVariable Integer coduni,
                                                      @Valid @RequestBody UnidadFotoRequest request,
                                                      Authentication authentication) {
        UnidadFotoResponse response = unidadFotoService.create(coduni, request, authentication);
        return created(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UnidadFotoResponse> createInternal(@PathVariable Integer coduni,
                                                              @RequestPart("foto") MultipartFile foto,
                                                              @RequestParam(required = false)
                                                              @Size(max = 150, message = "El título no puede superar 150 caracteres.")
                                                              String titulo,
                                                              @RequestParam(required = false)
                                                              @Size(max = 100, message = "El ambiente no puede superar 100 caracteres.")
                                                              String ambiente,
                                                              @RequestParam(required = false)
                                                              @NotNull(message = "El orden es obligatorio.")
                                                              @Min(value = 0, message = "El orden no puede ser negativo.")
                                                              Integer orden,
                                                              Authentication authentication) {
        UnidadFotoResponse response = unidadFotoService.createInternal(coduni, foto,
                new UnidadFotoMetadataRequest(titulo, ambiente, orden), authentication);
        return created(response);
    }

    private ResponseEntity<UnidadFotoResponse> created(UnidadFotoResponse response) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<UnidadFotoResponse> listByUnidad(@PathVariable Integer coduni, Authentication authentication) {
        return unidadFotoService.listByUnidad(coduni, authentication);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UnidadFotoResponse update(@PathVariable Integer coduni, @PathVariable Integer id,
                                     @Valid @RequestBody UnidadFotoRequest request,
                                     Authentication authentication) {
        return unidadFotoService.update(coduni, id, request, authentication);
    }

    @PatchMapping(value = "/{id}/metadata", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UnidadFotoResponse updateMetadata(@PathVariable Integer coduni, @PathVariable Integer id,
                                             @Valid @RequestBody UnidadFotoMetadataRequest request,
                                             Authentication authentication) {
        return unidadFotoService.updateMetadata(coduni, id, request, authentication);
    }

    @PutMapping(value = "/{id}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UnidadFotoResponse replaceArchivo(@PathVariable Integer coduni, @PathVariable Integer id,
                                             @RequestPart("foto") MultipartFile foto,
                                             Authentication authentication) {
        return unidadFotoService.replaceArchivo(coduni, id, foto, authentication);
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> getArchivo(@PathVariable Integer coduni, @PathVariable Integer id,
                                                Authentication authentication) {
        UnidadFotoService.UnidadFotoResource archivo = unidadFotoService.getArchivo(coduni, id, authentication);
        return ResponseEntity.ok().contentType(archivo.mediaType()).body(archivo.resource());
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
