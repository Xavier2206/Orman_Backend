package com.orman.backend.notification.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.notification.dto.response.NotificacionResponse;
import com.orman.backend.notification.dto.response.NotificacionResumenResponse;
import com.orman.backend.notification.entity.NotificacionEntity;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.mapper.NotificacionMapper;
import com.orman.backend.notification.repository.NotificacionRepository;
import com.orman.backend.notification.service.NotificacionService;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionMapper notificacionMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificacionResponse> list(NotificacionTipo tipo, Boolean leida, Pageable pageable,
                                                   Authentication authentication) {
        Page<NotificacionResponse> page = notificacionRepository.searchOwn(authenticatedLogin(authentication), tipo, leida,
                defaultSort(pageable)).map(notificacionMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacionResponse get(Long codnot, Authentication authentication) {
        return notificacionMapper.toResponse(findOwn(codnot, authenticatedLogin(authentication)));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacionResumenResponse summary(Authentication authentication) {
        return new NotificacionResumenResponse(
                notificacionRepository.countByDestinatarioLoginAndFechaLecturaIsNull(authenticatedLogin(authentication)));
    }

    @Override
    @Transactional
    public NotificacionResponse markAsRead(Long codnot, Authentication authentication) {
        NotificacionEntity notificacion = notificacionRepository.findOwnForUpdate(codnot, authenticatedLogin(authentication))
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada."));
        if (notificacion.getFechaLectura() == null) {
            notificacion.setFechaLectura(OrmanTimeConfig.businessNow(clock));
        }
        return notificacionMapper.toResponse(notificacion);
    }

    private NotificacionEntity findOwn(Long codnot, String login) {
        return notificacionRepository.findByCodnotAndDestinatarioLogin(codnot, login)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada."));
    }

    private Pageable defaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("codnot")));
    }

    private String authenticatedLogin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("No tiene autorización para consultar sus Notificaciones.");
        }
        return user.login();
    }
}
