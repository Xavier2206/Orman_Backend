package com.orman.backend.health.controller;

import com.orman.backend.health.dto.SaludResponse;
import com.orman.backend.health.service.SaludService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/salud")
@RequiredArgsConstructor
public class SaludController {

    private final SaludService saludService;

    @GetMapping
    public ResponseEntity<SaludResponse> consultarSalud() {
        boolean baseDatosDisponible = saludService.comprobarBaseDatos();
        SaludResponse response = new SaludResponse(
                baseDatosDisponible ? "UP" : "DOWN",
                baseDatosDisponible ? "UP" : "DOWN");

        return ResponseEntity.status(baseDatosDisponible ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
