package com.orman.backend.config;

import com.orman.backend.contract.service.impl.ContratoActivacionScheduler;
import com.orman.backend.notification.service.impl.CuotaNotificacionScheduler;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(OrmanTimeConfig.class)
class OrmanTimeConfigTest {

    @Autowired
    private Clock clock;

    @Autowired
    private ZoneId ormanZone;

    @Test
    void exposesLaPazAsTheOfficialZoneAndClockZone() {
        assertThat(ormanZone.getId()).isEqualTo("America/La_Paz");
        assertThat(OrmanTimeConfig.ORMAN_ZONE).isEqualTo(ormanZone);
        assertThat(clock.getZone()).isEqualTo(ormanZone);
        assertThat(ormanZone.getRules().getOffset(Instant.parse("2026-09-29T23:05:00Z")))
                .isEqualTo(ZoneOffset.ofHours(-4));
    }

    @Test
    void derivesTodayFromTheBolivianCalendarEvenWhenGivenAUtcClock() {
        Clock fixedUtcClock = Clock.fixed(Instant.parse("2026-09-30T02:30:00Z"), ZoneOffset.UTC);

        assertThat(OrmanTimeConfig.today(fixedUtcClock)).isEqualTo(LocalDate.of(2026, 9, 29));
    }

    @Test
    void storesBusinessWallTimeInBoliviaAndKeepsTechnicalTimeInUtc() {
        Clock fixedUtcClock = Clock.fixed(Instant.parse("2026-09-30T16:20:00Z"), ZoneOffset.UTC);

        assertThat(OrmanTimeConfig.businessNow(fixedUtcClock))
                .isEqualTo(LocalDateTime.of(2026, 9, 30, 12, 20));
        assertThat(OrmanTimeConfig.technicalNow(fixedUtcClock))
                .isEqualTo(LocalDateTime.of(2026, 9, 30, 16, 20));
    }

    @Test
    void schedulesRunInTheOfficialBolivianZone() throws NoSuchMethodException {
        Scheduled contractSchedule = ContratoActivacionScheduler.class.getMethod("activateDaily")
                .getAnnotation(Scheduled.class);
        Scheduled quotaSchedule = CuotaNotificacionScheduler.class.getMethod("generateDaily")
                .getAnnotation(Scheduled.class);

        assertThat(contractSchedule.zone()).isEqualTo(OrmanTimeConfig.ORMAN_ZONE_ID);
        assertThat(quotaSchedule.zone()).isEqualTo(OrmanTimeConfig.ORMAN_ZONE_ID);
    }
}
