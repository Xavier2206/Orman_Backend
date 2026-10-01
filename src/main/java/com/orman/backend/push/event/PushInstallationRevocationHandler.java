package com.orman.backend.push.event;

import com.orman.backend.auth.event.SesionesRevocadasEvent;
import com.orman.backend.push.service.PushInstallationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PushInstallationRevocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushInstallationRevocationHandler.class);

    private final PushInstallationService pushInstallationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(SesionesRevocadasEvent event) {
        try {
            pushInstallationService.deactivateSessions(event.sids());
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudieron desactivar destinos push de sesiones revocadas; count={} error={}",
                    event.sids().size(), exception.getClass().getSimpleName());
        }
    }
}
