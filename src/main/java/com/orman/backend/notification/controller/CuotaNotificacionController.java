package com.orman.backend.notification.controller;

import com.orman.backend.notification.dto.response.NotificacionResponse;
import com.orman.backend.notification.service.NotificacionGeneracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuotas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class CuotaNotificacionController {

    private final NotificacionGeneracionService notificacionGeneracionService;

    @PostMapping("/{codcuo}/notificar")
    public NotificacionResponse notifyPendingPayment(@PathVariable Integer codcuo, Authentication authentication) {
        return notificacionGeneracionService.notifyPendingPayment(codcuo, authentication);
    }
}
