package com.orman.backend.notification.service.impl;

import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.notification.service.NotificacionGeneracionService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CuotaNotificacionScheduler {

    private final CuotaRepository cuotaRepository;
    private final NotificacionGeneracionService notificacionGeneracionService;
    private final Clock clock;

    @Scheduled(cron = "${notification.scheduler.cron:0 0 8 * * *}",
            zone = OrmanTimeConfig.ORMAN_ZONE_ID)
    @Transactional
    public void generateDaily() {
        generateFor(OrmanTimeConfig.today(clock));
    }

    @Transactional
    public void generateFor(LocalDate fechaActual) {
        List<CuotaEntity> cuotas = cuotaRepository.findAllPendingOrPartialDueOnOrBefore(fechaActual.plusDays(1),
                List.of(CuotaEstado.PENDIENTE, CuotaEstado.PARCIAL));
        for (CuotaEntity cuota : cuotas) {
            if (cuota.getFechaVencimiento().isBefore(fechaActual)) {
                notificacionGeneracionService.generateOverdueQuota(cuota.getCodcuo());
            } else {
                notificacionGeneracionService.generateUpcomingQuota(cuota.getCodcuo(), fechaActual);
            }
        }
    }
}
