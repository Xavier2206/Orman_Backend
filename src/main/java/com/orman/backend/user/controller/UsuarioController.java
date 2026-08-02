package com.orman.backend.user.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.user.dto.ChangePasswordRequest;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UpdateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody CreateUsuarioRequest request) {
        UsuarioResponse response = usuarioService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{login}")
                .buildAndExpand(response.login()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{login}")
    public UsuarioResponse get(@PathVariable String login) {
        return usuarioService.get(login);
    }

    @GetMapping
    public PageResponse<UsuarioResponse> list(
            @PageableDefault(page = 0, size = 20, sort = "login", direction = Sort.Direction.ASC) Pageable pageable) {
        return usuarioService.list(pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable);
    }

    @PutMapping("/{login}")
    public UsuarioResponse update(@PathVariable String login, @Valid @RequestBody UpdateUsuarioRequest request) {
        return usuarioService.update(login, request);
    }

    @PatchMapping("/{login}/desactivar")
    public UsuarioResponse deactivate(@PathVariable String login) {
        return usuarioService.deactivate(login);
    }

    @PatchMapping("/{login}/activar")
    public UsuarioResponse activate(@PathVariable String login) {
        return usuarioService.activate(login);
    }

    @PutMapping("/{login}/password")
    public ResponseEntity<Void> changePassword(@PathVariable String login,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        usuarioService.changePassword(login, request);
        return ResponseEntity.noContent().build();
    }
}
