package com.orman.backend.config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Central time policy for ORMAN's Bolivia-only business calendar. */
@Configuration
public class OrmanTimeConfig {

    public static final String ORMAN_ZONE_ID = "America/La_Paz";
    public static final ZoneId ORMAN_ZONE = ZoneId.of(ORMAN_ZONE_ID);

    /** Naive timestamp columns for technical instants are kept as UTC wall time for compatibility. */
    public static final ZoneOffset TECHNICAL_OFFSET = ZoneOffset.UTC;

    @Bean
    ZoneId ormanZone() {
        return ORMAN_ZONE;
    }

    @Bean
    Clock clock() {
        return Clock.system(ORMAN_ZONE);
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ORMAN_ZONE));
    }

    public static LocalDateTime businessNow(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), ORMAN_ZONE);
    }

    public static LocalDateTime technicalNow(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), TECHNICAL_OFFSET);
    }

    /** Interprets a legacy UTC TIMESTAMP value as the instant it represents and presents it in Bolivia. */
    public static OffsetDateTime technicalUtcToOrman(LocalDateTime utcValue) {
        return utcValue == null ? null : utcValue.atOffset(TECHNICAL_OFFSET)
                .atZoneSameInstant(ORMAN_ZONE).toOffsetDateTime();
    }

    /** Interprets a wall-clock value known to have been entered in Bolivia. */
    public static OffsetDateTime ormanLocalToOffset(LocalDateTime localValue) {
        return localValue == null ? null : localValue.atZone(ORMAN_ZONE).toOffsetDateTime();
    }

    public static OffsetDateTime instantToOrman(Instant instant) {
        return instant == null ? null : instant.atZone(ORMAN_ZONE).toOffsetDateTime();
    }
}
