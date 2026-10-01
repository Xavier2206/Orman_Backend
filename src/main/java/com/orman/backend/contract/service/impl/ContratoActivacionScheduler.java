package com.orman.backend.contract.service.impl;

import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.contract.repository.ContratoRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContratoActivacionScheduler {

    private final ContratoRepository contratoRepository;
    private final Clock clock;

    @Scheduled(cron = "${contract.scheduler.cron:0 5 0 * * *}",
            zone = OrmanTimeConfig.ORMAN_ZONE_ID)
    @Transactional
    public void activateDaily() {
        activateFor(OrmanTimeConfig.today(clock));
    }

    @Transactional
    public int activateFor(LocalDate fechaActual) {
        return contratoRepository.activateScheduled(fechaActual);
    }
}
