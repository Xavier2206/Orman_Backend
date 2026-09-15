package com.orman.backend.contract.service.impl;

import com.orman.backend.contract.repository.ContratoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContratoActivacionScheduler {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/La_Paz");

    private final ContratoRepository contratoRepository;
    private final Clock clock;

    @Scheduled(cron = "${contract.scheduler.cron:0 5 0 * * *}",
            zone = "${contract.scheduler.zone:America/La_Paz}")
    @Transactional
    public void activateDaily() {
        activateFor(LocalDate.now(clock.withZone(ZONA_NEGOCIO)));
    }

    @Transactional
    public int activateFor(LocalDate fechaActual) {
        return contratoRepository.activateScheduled(fechaActual);
    }
}
