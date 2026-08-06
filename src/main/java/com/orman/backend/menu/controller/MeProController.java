package com.orman.backend.menu.controller;

import com.orman.backend.menu.dto.response.MeProResponse;
import com.orman.backend.menu.service.MeProService;
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
public class MeProController {
    private final MeProService meProService;
    @PostMapping("/menus/{codm}/procesos/{codp}") public ResponseEntity<MeProResponse> assign(@PathVariable Integer codm, @PathVariable Integer codp) {
        MeProResponse response = meProService.assign(codm, codp);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(response);
    }
    @DeleteMapping("/menus/{codm}/procesos/{codp}") public ResponseEntity<Void> remove(@PathVariable Integer codm, @PathVariable Integer codp) { meProService.remove(codm, codp); return ResponseEntity.noContent().build(); }
    @GetMapping("/menus/{codm}/procesos") public List<MeProResponse> listByMenu(@PathVariable Integer codm) { return meProService.listByMenu(codm); }
    @GetMapping("/procesos/{codp}/menus") public List<MeProResponse> listByProceso(@PathVariable Integer codp) { return meProService.listByProceso(codp); }
}
