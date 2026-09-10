package com.orman.backend.notification.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.notification.dto.response.NotificacionResponse;
import com.orman.backend.notification.dto.response.NotificacionResumenResponse;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.service.NotificacionService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PROPIETARIO')")
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public PageResponse<NotificacionResponse> list(@RequestParam(required = false)
                                                   @Pattern(regexp = "CUOTA_PROXIMA_VENCER|CUOTA_VENCIDA|COMPROBANTE_RECIBIDO|PAGO_CONFIRMADO|PAGO_RECHAZADO",
                                                           message = "El tipo de Notificación no es válido.") String tipo,
                                                   @RequestParam(required = false) Boolean leida,
                                                   @PageableDefault(page = 0, size = 20, sort = "fechaCreacion",
                                                           direction = Sort.Direction.DESC) Pageable pageable,
                                                   Authentication authentication) {
        return notificacionService.list(tipo == null ? null : NotificacionTipo.valueOf(tipo), leida, limit(pageable),
                authentication);
    }

    @GetMapping("/resumen")
    public NotificacionResumenResponse summary(Authentication authentication) {
        return notificacionService.summary(authentication);
    }

    @GetMapping("/{codnot}")
    public NotificacionResponse get(@PathVariable Long codnot, Authentication authentication) {
        return notificacionService.get(codnot, authentication);
    }

    @PatchMapping("/{codnot}/leer")
    public NotificacionResponse markAsRead(@PathVariable Long codnot, Authentication authentication) {
        return notificacionService.markAsRead(codnot, authentication);
    }

    private Pageable limit(Pageable pageable) {
        return pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort()) : pageable;
    }
}
