package com.orman.backend.person.controller;

import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.PersonaResumenResponse;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.person.dto.PersonaSearchCriteria;
import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.person.service.PersonaPhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
public class PersonaController {
    private final PersonaService personaService;
    private final PersonaPhotoService personaPhotoService;
    @PostMapping
    @PreAuthorize("hasAnyRole('PROPIETARIO', 'ADMINISTRADOR')")
    public ResponseEntity<PersonaResponse> create(@Valid @RequestBody CreatePersonaRequest request, Authentication authentication) {
        PersonaResponse response = personaService.create(request, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codper}").buildAndExpand(response.codper()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('PROPIETARIO', 'ADMINISTRADOR')")
    public PersonaResumenResponse resumen() {
        return personaService.resumen();
    }

    @GetMapping("/{codper}")
    @PreAuthorize("@authorizationService.canManagePerson(authentication, #codper)")
    public PersonaResponse get(@PathVariable Integer codper, Authentication authentication) {
        return personaService.get(codper, authentication);
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('PROPIETARIO', 'ADMINISTRADOR')")
    public PageResponse<PersonaResponse> list(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) String tipoPersona,
                                              @RequestParam(required = false) String estado,
                                              @PageableDefault(page = 0, size = 20, sort = "codper", direction = Sort.Direction.ASC) Pageable pageable,
                                              Authentication authentication) {
        Pageable limited = pageable.getPageSize() > 100 ? org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 100, pageable.getSort()) : pageable;
        return personaService.list(PersonaSearchCriteria.from(q, tipoPersona, estado), limited, authentication);
    }
    @PutMapping("/{codper}")
    @PreAuthorize("@authorizationService.canManagePerson(authentication, #codper)")
    public PersonaResponse update(@PathVariable Integer codper, @Valid @RequestBody UpdatePersonaRequest request,
                                  Authentication authentication) {
        return personaService.update(codper, request, authentication);
    }
    @PatchMapping("/{codper}/desactivar")
    @PreAuthorize("@authorizationService.canManagePerson(authentication, #codper)")
    public PersonaResponse deactivate(@PathVariable Integer codper, Authentication authentication) {
        return personaService.deactivate(codper, authentication);
    }
    @PatchMapping("/{codper}/activar")
    @PreAuthorize("@authorizationService.canManagePerson(authentication, #codper)")
    public PersonaResponse activate(@PathVariable Integer codper, Authentication authentication) {
        return personaService.activate(codper, authentication);
    }
    @DeleteMapping("/{codper}")
    @PreAuthorize("@authorizationService.canManagePerson(authentication, #codper)")
    public ResponseEntity<Void> delete(@PathVariable Integer codper) {
        personaService.delete(codper); return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{codper}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authorizationService.canManagePerson(authentication, #codper)")
    public ResponseEntity<Void> uploadPhoto(@PathVariable Integer codper, @RequestPart("foto") MultipartFile foto) {
        personaPhotoService.upload(codper, foto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{codper}/foto")
    @PreAuthorize("@authorizationService.canManagePerson(authentication, #codper)")
    public ResponseEntity<Resource> getPhoto(@PathVariable Integer codper) {
        PersonaPhotoService.PersonaPhotoResource photo = personaPhotoService.get(codper);
        return ResponseEntity.ok().contentType(photo.mediaType()).body(photo.resource());
    }

    @DeleteMapping("/{codper}/foto")
    @PreAuthorize("@authorizationService.canManagePerson(authentication, #codper)")
    public ResponseEntity<Void> deletePhoto(@PathVariable Integer codper) {
        personaPhotoService.delete(codper);
        return ResponseEntity.noContent().build();
    }
}
