package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.dto.response.SessionResponse;
import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.auth.exception.ExpiredSessionException;
import com.orman.backend.auth.exception.InvalidJwtException;
import com.orman.backend.auth.exception.RevokedSessionException;
import com.orman.backend.auth.mapper.SessionMapper;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private static final short ACTIVE = 1;

    private final SesionUsuarioRepository sessionRepository;
    private final SessionMapper sessionMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser authenticate(String login, UUID sid) {
        SesionUsuario session = sessionRepository.findForAuthentication(sid)
                .orElseThrow(InvalidJwtException::new);
        if (!session.getUsuario().getLogin().equals(login)) {
            throw new InvalidJwtException();
        }
        if (session.isRevoked()) {
            throw new RevokedSessionException();
        }
        if (session.isExpiredAt(nowUtc())) {
            throw new ExpiredSessionException();
        }
        try {
            if (!Short.valueOf(ACTIVE).equals(session.getUsuario().getEstado())
                    || session.getUsuario().getPersona() == null
                    || !Short.valueOf(ACTIVE).equals(session.getUsuario().getPersona().getEstado())) {
                throw new InvalidJwtException();
            }
        } catch (EntityNotFoundException exception) {
            throw new InvalidJwtException();
        }
        return new AuthenticatedUser(login, sid);
    }

    @Override
    @Transactional
    public void logout(AuthenticatedUser user) {
        SesionUsuario session = sessionRepository.findOwnedForUpdate(user.sid(), user.login())
                .orElseThrow(InvalidJwtException::new);
        session.revoke(nowUtc(), RevocationReason.LOGOUT);
    }

    @Override
    @Transactional
    public void logoutAll(AuthenticatedUser user) {
        revokeAll(user.login(), RevocationReason.LOGOUT_ALL);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> list(AuthenticatedUser user) {
        LocalDateTime now = nowUtc();
        return sessionRepository.findAllByUsuarioLoginAndFechaRevocacionIsNullOrderByFechaCreacionDesc(user.login())
                .stream()
                .filter(session -> !session.isExpiredAt(now))
                .map(session -> sessionMapper.toResponse(session, user.sid()))
                .toList();
    }

    @Override
    @Transactional
    public void revoke(AuthenticatedUser user, UUID sid) {
        SesionUsuario session = sessionRepository.findOwnedForUpdate(sid, user.login())
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada."));
        session.revoke(nowUtc(), RevocationReason.ADMIN_REVOKED);
    }

    @Override
    @Transactional
    public void revokeAll(String login, RevocationReason reason) {
        sessionRepository.revokeAllActive(login, nowUtc(), reason);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
