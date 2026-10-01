package com.orman.backend.push.event;

import com.orman.backend.auth.event.SesionesRevocadasEvent;
import com.orman.backend.push.service.PushInstallationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PushInstallationRevocationHandlerTest {

    @Mock private PushInstallationService pushInstallationService;
    @InjectMocks private PushInstallationRevocationHandler handler;

    @Test
    void deactivationFailureDoesNotEscapeAfterSessionRevocationCommitted() {
        List<UUID> sids = List.of(UUID.randomUUID());
        doThrow(new IllegalStateException("database unavailable"))
                .when(pushInstallationService).deactivateSessions(sids);

        assertThatCode(() -> handler.on(new SesionesRevocadasEvent(sids))).doesNotThrowAnyException();

        verify(pushInstallationService).deactivateSessions(sids);
    }
}
