package com.orman.backend.push.service.impl;

import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.auth.exception.ExpiredSessionException;
import com.orman.backend.auth.exception.InvalidJwtException;
import com.orman.backend.auth.exception.RevokedSessionException;
import com.orman.backend.auth.exception.SecurityAccessDeniedException;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.push.dto.PushInstallationRequest;
import com.orman.backend.push.entity.DispositivoPushEntity;
import com.orman.backend.push.repository.DispositivoPushRepository;
import com.orman.backend.push.service.PushInstallationService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushInstallationServiceImpl implements PushInstallationService {

    private static final short ACTIVE = 1;
    private static final String REGISTRATION_LOCK_SQL = "SELECT pg_advisory_xact_lock(20210929, 1)";

    private final DispositivoPushRepository pushRepository;
    private final SesionUsuarioRepository sessionRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    @Override
    @Transactional
    public void register(AuthenticatedUser authenticatedUser, PushInstallationRequest request) {
        acquireRegistrationLock();
        SesionUsuario session = requireMobileSession(authenticatedUser);
        LocalDateTime now = nowUtc();
        String installationId = request.installationId().trim();

        DispositivoPushEntity currentForSession = pushRepository.findBySidForUpdate(session.getSid()).orElse(null);
        DispositivoPushEntity target = pushRepository.findByInstallationId(installationId).orElse(null);

        if (currentForSession != null && currentForSession.equals(target)) {
            currentForSession.replaceInstallation(installationId, request.platform(), now);
            return;
        }

        if (currentForSession != null && target != null) {
            pushRepository.deleteByCoddis(currentForSession.getCoddis());
            pushRepository.flush();
            target.associate(session, request.platform(), now);
            pushRepository.saveAndFlush(target);
            return;
        }

        if (currentForSession != null) {
            currentForSession.replaceInstallation(installationId, request.platform(), now);
            pushRepository.saveAndFlush(currentForSession);
            return;
        }

        if (target != null) {
            target.associate(session, request.platform(), now);
            pushRepository.saveAndFlush(target);
            return;
        }

        pushRepository.saveAndFlush(new DispositivoPushEntity(session, installationId, request.platform(), now));
    }

    @Override
    @Transactional
    public void deactivate(AuthenticatedUser authenticatedUser) {
        acquireRegistrationLock();
        SesionUsuario session = requireMobileSession(authenticatedUser);
        pushRepository.findBySidForUpdate(session.getSid()).ifPresent(device -> device.deactivate(nowUtc()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deactivateSessions(List<UUID> sids) {
        if (sids == null || sids.isEmpty()) {
            return;
        }
        pushRepository.deactivateBySids(sids, nowUtc());
    }

    private SesionUsuario requireMobileSession(AuthenticatedUser authenticatedUser) {
        SesionUsuario session = sessionRepository.findBySidForUpdate(authenticatedUser.sid())
                .orElseThrow(InvalidJwtException::new);
        if (!session.getUsuario().getLogin().equals(authenticatedUser.login())) {
            throw new InvalidJwtException();
        }
        if (session.isRevoked()) {
            throw new RevokedSessionException();
        }
        if (session.isExpiredAt(nowUtc())) {
            throw new ExpiredSessionException();
        }
        if (session.getClientType() != ClientType.MOBILE) {
            throw new SecurityAccessDeniedException();
        }
        try {
            if (!Short.valueOf(ACTIVE).equals(session.getUsuario().getEstado())
                    || session.getUsuario().getPersona() == null
                    || !Short.valueOf(ACTIVE).equals(session.getUsuario().getPersona().getEstado())) {
                throw new InvalidJwtException();
            }
        } catch (jakarta.persistence.EntityNotFoundException exception) {
            throw new InvalidJwtException();
        }
        return session;
    }

    private void acquireRegistrationLock() {
        // El registro es infrecuente; este lock serializa la reasociación global del installation ID.
        entityManager.createNativeQuery(REGISTRATION_LOCK_SQL).getSingleResult();
    }

    private LocalDateTime nowUtc() {
        return OrmanTimeConfig.technicalNow(clock);
    }
}
