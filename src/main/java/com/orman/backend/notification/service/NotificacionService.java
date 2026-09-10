package com.orman.backend.notification.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.notification.dto.response.NotificacionResponse;
import com.orman.backend.notification.dto.response.NotificacionResumenResponse;
import com.orman.backend.notification.entity.NotificacionTipo;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface NotificacionService {

    PageResponse<NotificacionResponse> list(NotificacionTipo tipo, Boolean leida, Pageable pageable,
                                            Authentication authentication);

    NotificacionResponse get(Long codnot, Authentication authentication);

    NotificacionResumenResponse summary(Authentication authentication);

    NotificacionResponse markAsRead(Long codnot, Authentication authentication);
}
