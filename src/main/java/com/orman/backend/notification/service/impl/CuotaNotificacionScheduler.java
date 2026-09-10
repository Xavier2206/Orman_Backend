package com.orman.backend.notification.service.impl;

import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.notification.service.NotificacionGeneracionService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CuotaNotificacionScheduler {

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/La_Paz");

    private final CuotaRepository cuotaRepository;
    private final NotificacionGeneracionService notificacionGeneracionService;

    @Scheduled(cron = "${notification.scheduler.cron:0 0 8 * * *}",
            zone = "${notification.scheduler.zone:America/La_Paz}")
    @Transactional
    public void generateDaily() {
        generateFor(LocalDate.now(ZONA_HORARIA));
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
