package com.orman.backend.person.controller;

import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.person.service.PersonaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
public class PersonaController {
    private final PersonaService personaService;
    @PostMapping public ResponseEntity<PersonaResponse> create(@Valid @RequestBody CreatePersonaRequest request) {
        PersonaResponse response = personaService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codper}").buildAndExpand(response.codper()).toUri();
        return ResponseEntity.created(location).body(response);
    }
    @GetMapping("/{codper}") public PersonaResponse get(@PathVariable Integer codper) {
        return personaService.get(codper);
    }
    @GetMapping public PageResponse<PersonaResponse> list(@PageableDefault(page = 0, size = 20, sort = "codper", direction = Sort.Direction.ASC) Pageable pageable) {
        return personaService.list(pageable.getPageSize() > 100 ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort()) : pageable);
    }
    @PutMapping("/{codper}") public PersonaResponse update(@PathVariable Integer codper, @Valid @RequestBody UpdatePersonaRequest request) {
        return personaService.update(codper, request);
    }
    @PatchMapping("/{codper}/desactivar") public PersonaResponse deactivate(@PathVariable Integer codper) {
        return personaService.deactivate(codper);
    }
    @PatchMapping("/{codper}/activar") public PersonaResponse activate(@PathVariable Integer codper) {
        return personaService.activate(codper);
    }
    @DeleteMapping("/{codper}") public ResponseEntity<Void> delete(@PathVariable Integer codper) {
        personaService.delete(codper); return ResponseEntity.noContent().build();
    }
}
