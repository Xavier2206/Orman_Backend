package com.orman.backend.role.controller;

import com.orman.backend.role.dto.response.RolMeResponse;
import com.orman.backend.role.service.RolMeService;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class RolMeController {
    private final RolMeService rolMeService;
    @PostMapping("/roles/{codr}/menus/{codm}") public ResponseEntity<RolMeResponse> assign(@PathVariable Integer codr, @PathVariable Integer codm) {
        RolMeResponse response = rolMeService.assign(codr, codm);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(response);
    }
    @DeleteMapping("/roles/{codr}/menus/{codm}") public ResponseEntity<Void> remove(@PathVariable Integer codr, @PathVariable Integer codm) { rolMeService.remove(codr, codm); return ResponseEntity.noContent().build(); }
    @GetMapping("/roles/{codr}/menus") public List<RolMeResponse> listByRol(@PathVariable Integer codr) { return rolMeService.listByRol(codr); }
    @GetMapping("/menus/{codm}/roles") public List<RolMeResponse> listByMenu(@PathVariable Integer codm) { return rolMeService.listByMenu(codm); }
}
