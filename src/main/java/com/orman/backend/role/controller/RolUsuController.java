package com.orman.backend.role.controller;

import com.orman.backend.role.dto.response.RolUsuResponse;
import com.orman.backend.role.service.RolUsuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RolUsuController {

    private final RolUsuService rolUsuService;

    @PostMapping("/usuarios/{login}/roles/{codr}")
    public ResponseEntity<RolUsuResponse> assign(@PathVariable String login, @PathVariable Integer codr) {
        RolUsuResponse response = rolUsuService.assign(login, codr);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/usuarios/{login}/roles/{codr}")
    public ResponseEntity<Void> remove(@PathVariable String login, @PathVariable Integer codr) {
        rolUsuService.remove(login, codr);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuarios/{login}/roles")
    public List<RolUsuResponse> listByUsuario(@PathVariable String login) {
        return rolUsuService.listByUsuario(login);
    }

    @GetMapping("/roles/{codr}/usuarios")
    public List<RolUsuResponse> listByRol(@PathVariable Integer codr) {
        return rolUsuService.listByRol(codr);
    }
}
