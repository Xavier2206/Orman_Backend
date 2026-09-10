package com.orman.backend.notification.service;

import com.orman.backend.notification.dto.response.NotificacionResponse;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;

public interface NotificacionGeneracionService {

    void generatePaymentConfirmed(Integer codpag);

    void generatePaymentRejected(Integer codpag);

    void generateComprobanteReceived(Integer codpag);

    void generateUpcomingQuota(Integer codcuo, LocalDate fechaActual);

    void generateOverdueQuota(Integer codcuo);

    NotificacionResponse notifyPendingPayment(Integer codcuo, Authentication authentication);
}
