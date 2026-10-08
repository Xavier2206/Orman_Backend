package com.orman.backend.dashboard.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.dashboard.mapper.DashboardMapper;
import com.orman.backend.dashboard.repository.DashboardIngresoMensualProjection;
import com.orman.backend.dashboard.repository.DashboardIngresoPropiedadProjection;
import com.orman.backend.dashboard.repository.DashboardPropiedadProjection;
import com.orman.backend.dashboard.repository.DashboardRepository;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final Integer CODPER = 41;
    private static final YearMonth CURRENT_MONTH = YearMonth.of(2026, 10);

    @Mock private DashboardRepository dashboardRepository;
    @Mock private PropertyOwnershipService propertyOwnershipService;
    @Mock private Persona persona;
    @Mock private DashboardPropiedadProjection firstProperty;
    @Mock private DashboardPropiedadProjection secondProperty;
    @Mock private DashboardIngresoPropiedadProjection firstIncome;
    @Mock private DashboardIngresoPropiedadProjection secondIncome;
    @Mock private DashboardIngresoMensualProjection monthlyIncome;

    private DashboardServiceImpl service;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-07T16:00:00Z"), ZoneId.of("America/La_Paz"));
        service = new DashboardServiceImpl(dashboardRepository, propertyOwnershipService,
                new DashboardMapper(), clock);
        authentication = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser("dashboard.owner", UUID.randomUUID()), null, List.of());
        when(persona.getCodper()).thenReturn(CODPER);
        when(propertyOwnershipService.currentPropietaria(authentication)).thenReturn(persona);
    }

    @Test
    void calculatesWeightedGeneralRecoveryAndKeepsRecoveryAboveOneHundredPercent() {
        when(firstProperty.getCodprop()).thenReturn(1);
        when(firstProperty.getNombre()).thenReturn("Propiedad Uno");
        when(firstProperty.getInversionInicial()).thenReturn(new BigDecimal("100.00"));
        when(secondProperty.getCodprop()).thenReturn(2);
        when(secondProperty.getNombre()).thenReturn("Propiedad Dos");
        when(secondProperty.getInversionInicial()).thenReturn(new BigDecimal("300.00"));
        when(firstIncome.getCodprop()).thenReturn(1);
        when(firstIncome.getTotal()).thenReturn(new BigDecimal("150.00"));
        when(secondIncome.getCodprop()).thenReturn(2);
        when(secondIncome.getTotal()).thenReturn(new BigDecimal("120.00"));
        when(monthlyIncome.getCodprop()).thenReturn(1);
        when(monthlyIncome.getAnio()).thenReturn(CURRENT_MONTH.getYear());
        when(monthlyIncome.getMes()).thenReturn(CURRENT_MONTH.getMonthValue());
        when(monthlyIncome.getTotal()).thenReturn(new BigDecimal("20.00"));
        when(dashboardRepository.findPropiedadesByPropietaria(CODPER)).thenReturn(List.of(firstProperty, secondProperty));
        when(dashboardRepository.sumarIngresosConfirmadosPorPropiedad(CODPER))
                .thenReturn(List.of(firstIncome, secondIncome));
        when(dashboardRepository.sumarIngresosMensualesConfirmados(CODPER,
                LocalDateTime.of(2025, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0)))
                .thenReturn(List.of(monthlyIncome));

        var response = service.resumenFinanciero(authentication);

        assertThat(response.moneda()).isEqualTo("BOB");
        assertThat(response.resumenGeneral().cantidadPropiedades()).isEqualTo(2);
        assertThat(response.resumenGeneral().inversionInicialTotal()).isEqualByComparingTo("400.00");
        assertThat(response.resumenGeneral().ingresosConfirmadosAcumulados()).isEqualByComparingTo("270.00");
        assertThat(response.resumenGeneral().porcentajeRecuperacion()).isEqualByComparingTo("67.50");
        assertThat(response.propiedades()).extracting("porcentajeRecuperacion")
                .containsExactly(new BigDecimal("150.00"), new BigDecimal("40.00"));
        assertThat(response.ingresosConfirmadosPorMes()).hasSize(12);
        assertThat(response.ingresosConfirmadosPorMes().getLast().periodo()).isEqualTo("2026-10");
        assertThat(response.ingresosConfirmadosPorMes().getLast().monto()).isEqualByComparingTo("20.00");
    }

    @Test
    void returnsNullRecoveryForZeroInvestmentAndZeroAmountsForNoConfirmedPayments() {
        when(firstProperty.getCodprop()).thenReturn(1);
        when(firstProperty.getNombre()).thenReturn("Sin inversión");
        when(firstProperty.getInversionInicial()).thenReturn(BigDecimal.ZERO);
        when(dashboardRepository.findPropiedadesByPropietaria(CODPER)).thenReturn(List.of(firstProperty));
        when(dashboardRepository.sumarIngresosConfirmadosPorPropiedad(CODPER)).thenReturn(List.of());
        when(dashboardRepository.sumarIngresosMensualesConfirmados(CODPER,
                LocalDateTime.of(2025, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0)))
                .thenReturn(List.of());

        var response = service.resumenFinanciero(authentication);

        assertThat(response.resumenGeneral().inversionInicialTotal()).isEqualByComparingTo("0.00");
        assertThat(response.resumenGeneral().ingresosConfirmadosAcumulados()).isEqualByComparingTo("0.00");
        assertThat(response.resumenGeneral().porcentajeRecuperacion()).isNull();
        assertThat(response.propiedades().getFirst().ingresosConfirmadosAcumulados()).isEqualByComparingTo("0.00");
        assertThat(response.propiedades().getFirst().porcentajeRecuperacion()).isNull();
        assertThat(response.propiedades().getFirst().ingresosConfirmadosPorMes()).hasSize(12)
                .allSatisfy(month -> assertThat(month.monto()).isEqualByComparingTo("0.00"));
    }

    @Test
    void returnsAnEmptyPropertyListAndNullGlobalRecoveryWhenOwnerHasNoProperties() {
        when(dashboardRepository.findPropiedadesByPropietaria(CODPER)).thenReturn(List.of());
        when(dashboardRepository.sumarIngresosConfirmadosPorPropiedad(CODPER)).thenReturn(List.of());

        var response = service.resumenFinanciero(authentication);

        assertThat(response.resumenGeneral().cantidadPropiedades()).isZero();
        assertThat(response.resumenGeneral().inversionInicialTotal()).isEqualByComparingTo("0.00");
        assertThat(response.resumenGeneral().ingresosConfirmadosAcumulados()).isEqualByComparingTo("0.00");
        assertThat(response.resumenGeneral().porcentajeRecuperacion()).isNull();
        assertThat(response.propiedades()).isEmpty();
        assertThat(response.ingresosConfirmadosPorMes()).hasSize(12)
                .allSatisfy(month -> assertThat(month.monto()).isEqualByComparingTo("0.00"));
    }
}
