package com.orman.backend.user.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.authorization.service.AuthorizationService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @PreAuthorize("@authorizationService.canManagePerson(authentication, #request.codper)")
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody CreateUsuarioRequest request) {
        UsuarioResponse response = usuarioService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{login}")
                .buildAndExpand(response.login()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{login}")
    @PreAuthorize("@authorizationService.canManageUser(authentication, #login)")
    public UsuarioResponse get(@PathVariable String login) {
        return usuarioService.get(login);
    }

    @GetMapping
    @PreAuthorize("hasRole('PROPIETARIO')")
    public PageResponse<UsuarioResponse> list(
            @RequestParam(required = false) String q,
            @PageableDefault(page = 0, size = 20, sort = "login", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication) {
        Pageable limited = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;
        boolean owner = authorizationService.isOwner(authentication);
        boolean hasQuery = q != null && !q.isBlank();
        if (owner) {
            return hasQuery ? usuarioService.list(q, limited) : usuarioService.list(limited);
        }
        return hasQuery ? usuarioService.listCommon(q, limited) : usuarioService.listCommon(limited);
    }

    @PutMapping("/{login}")
    @PreAuthorize("@authorizationService.canManageUser(authentication, #login)")
    public UsuarioResponse update(@PathVariable String login, @Valid @RequestBody UpdateUsuarioRequest request) {
        return usuarioService.update(login, request);
    }

    @PatchMapping("/{login}/desactivar")
    @PreAuthorize("@authorizationService.canManageUser(authentication, #login)")
    public UsuarioResponse deactivate(@PathVariable String login) {
        return usuarioService.deactivate(login);
    }

    @PatchMapping("/{login}/activar")
    @PreAuthorize("@authorizationService.canManageUser(authentication, #login)")
    public UsuarioResponse activate(@PathVariable String login) {
        return usuarioService.activate(login);
    }

    @PutMapping("/{login}/password")
    @PreAuthorize("@authorizationService.isSelfOrOwner(authentication, #login)")
    public ResponseEntity<Void> changePassword(@PathVariable String login,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        usuarioService.changePassword(login, request);
        return ResponseEntity.noContent().build();
    }
}
