package com.orman.backend.property.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.property.dto.request.PropiedadRequest;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.dto.response.PropiedadResumenResponse;
import com.orman.backend.property.service.PropiedadPortadaService;
import com.orman.backend.property.service.PropiedadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/propiedades")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PROPIETARIO')")
public class PropiedadController {

    private final PropiedadService propiedadService;
    private final PropiedadPortadaService propiedadPortadaService;

    @PostMapping
    public ResponseEntity<PropiedadResponse> create(@Valid @RequestBody PropiedadRequest request,
                                                     Authentication authentication) {
        PropiedadResponse response = propiedadService.create(request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codprop}")
                .buildAndExpand(response.codprop()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/resumen")
    public PropiedadResumenResponse resumen(Authentication authentication) {
        return propiedadService.resumen(authentication);
    }

    @GetMapping
    public PageResponse<PropiedadResponse> list(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false)
                                                 @Pattern(regexp = "(?i)CASA|EDIFICIO",
                                                         message = "El tipo debe ser CASA o EDIFICIO.") String tipo,
                                                 @RequestParam(required = false)
                                                 @Pattern(regexp = "0|1", message = "El estado debe ser 0 o 1.")
                                                 String estado,
                                                 @PageableDefault(page = 0, size = 20, sort = "nombre",
                                                         direction = Sort.Direction.ASC) Pageable pageable,
                                                 Authentication authentication) {
        return propiedadService.list(q, tipo, estado == null ? null : Short.valueOf(estado), limit(pageable),
                authentication);
    }

    @GetMapping("/{codprop}")
    public PropiedadResponse get(@PathVariable Integer codprop, Authentication authentication) {
        return propiedadService.get(codprop, authentication);
    }

    @PutMapping("/{codprop}")
    public PropiedadResponse update(@PathVariable Integer codprop, @Valid @RequestBody PropiedadRequest request,
                                    Authentication authentication) {
        return propiedadService.update(codprop, request, authentication);
    }

    @PutMapping(value = "/{codprop}/portada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadCover(@PathVariable Integer codprop,
                                             @RequestPart("foto") MultipartFile foto,
                                             Authentication authentication) {
        propiedadPortadaService.upload(codprop, foto, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{codprop}/portada")
    public ResponseEntity<Resource> getCover(@PathVariable Integer codprop, Authentication authentication) {
        PropiedadPortadaService.PropiedadPortadaResource portada = propiedadPortadaService.get(codprop, authentication);
        return ResponseEntity.ok().contentType(portada.mediaType()).body(portada.resource());
    }

    @DeleteMapping("/{codprop}/portada")
    public ResponseEntity<Void> deleteCover(@PathVariable Integer codprop, Authentication authentication) {
        propiedadPortadaService.delete(codprop, authentication);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{codprop}/activar")
    public PropiedadResponse activate(@PathVariable Integer codprop, Authentication authentication) {
        return propiedadService.activate(codprop, authentication);
    }

    @PatchMapping("/{codprop}/desactivar")
    public PropiedadResponse deactivate(@PathVariable Integer codprop, Authentication authentication) {
        return propiedadService.deactivate(codprop, authentication);
    }

    private Pageable limit(Pageable pageable) {
        return pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;
    }
}
