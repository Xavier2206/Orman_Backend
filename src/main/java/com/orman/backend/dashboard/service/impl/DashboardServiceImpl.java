package com.orman.backend.dashboard.service.impl;

import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.dashboard.dto.response.DashboardIngresoMensualResponse;
import com.orman.backend.dashboard.dto.response.DashboardPropiedadResponse;
import com.orman.backend.dashboard.dto.response.DashboardResumenFinancieroResponse;
import com.orman.backend.dashboard.mapper.DashboardMapper;
import com.orman.backend.dashboard.repository.DashboardIngresoMensualProjection;
import com.orman.backend.dashboard.repository.DashboardIngresoPropiedadProjection;
import com.orman.backend.dashboard.repository.DashboardPropiedadProjection;
import com.orman.backend.dashboard.repository.DashboardRepository;
import com.orman.backend.dashboard.service.DashboardService;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int PERCENTAGE_SCALE = 2;
    private static final int MONTH_COUNT = 12;

    private final DashboardRepository dashboardRepository;
    private final PropertyOwnershipService propertyOwnershipService;
    private final DashboardMapper dashboardMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public DashboardResumenFinancieroResponse resumenFinanciero(Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        List<DashboardPropiedadProjection> propiedades = dashboardRepository.findPropiedadesByPropietaria(codper);
        Map<Integer, BigDecimal> ingresosPorPropiedad = ingresosPorPropiedad(codper);

        YearMonth mesActual = YearMonth.now(clock.withZone(OrmanTimeConfig.ORMAN_ZONE));
        YearMonth primerMes = mesActual.minusMonths(MONTH_COUNT - 1L);
        List<YearMonth> periodos = IntStream.range(0, MONTH_COUNT)
                .mapToObj(primerMes::plusMonths)
                .toList();
        Map<Integer, Map<YearMonth, BigDecimal>> ingresosMensualesPorPropiedad =
                ingresosMensualesPorPropiedad(codper, propiedades, primerMes, mesActual.plusMonths(1));

        BigDecimal inversionTotal = ZERO_MONEY;
        BigDecimal ingresosTotal = ZERO_MONEY;
        List<DashboardPropiedadResponse> respuestasPropiedad = new ArrayList<>(propiedades.size());
        Map<YearMonth, BigDecimal> ingresosMensualesTotales = new HashMap<>();

        for (DashboardPropiedadProjection propiedad : propiedades) {
            BigDecimal inversion = money(propiedad.getInversionInicial());
            BigDecimal ingresos = money(ingresosPorPropiedad.getOrDefault(propiedad.getCodprop(), ZERO_MONEY));
            List<DashboardIngresoMensualResponse> meses = monthlySeries(
                    periodos, ingresosMensualesPorPropiedad.getOrDefault(propiedad.getCodprop(), Map.of()));
            meses.forEach(mes -> ingresosMensualesTotales.merge(
                    YearMonth.parse(mes.periodo()), mes.monto(), BigDecimal::add));

            inversionTotal = inversionTotal.add(inversion);
            ingresosTotal = ingresosTotal.add(ingresos);
            respuestasPropiedad.add(dashboardMapper.toPropiedad(propiedad, ingresos,
                    percentage(ingresos, inversion), meses));
        }

        List<DashboardIngresoMensualResponse> ingresosMensuales = monthlySeries(periodos, ingresosMensualesTotales);
        return dashboardMapper.toResponse(propiedades.size(), money(inversionTotal), money(ingresosTotal),
                percentage(ingresosTotal, inversionTotal), respuestasPropiedad, ingresosMensuales);
    }

    private Map<Integer, BigDecimal> ingresosPorPropiedad(Integer codper) {
        Map<Integer, BigDecimal> result = new HashMap<>();
        for (DashboardIngresoPropiedadProjection row : dashboardRepository.sumarIngresosConfirmadosPorPropiedad(codper)) {
            result.put(row.getCodprop(), money(row.getTotal()));
        }
        return result;
    }

    private Map<Integer, Map<YearMonth, BigDecimal>> ingresosMensualesPorPropiedad(
            Integer codper, List<DashboardPropiedadProjection> propiedades, YearMonth desde, YearMonth hasta) {
        if (propiedades.isEmpty()) {
            return Map.of();
        }

        LocalDateTime desdeInclusive = desde.atDay(1).atStartOfDay();
        LocalDateTime hastaExclusive = hasta.atDay(1).atStartOfDay();
        Map<Integer, Map<YearMonth, BigDecimal>> result = new HashMap<>();
        for (DashboardIngresoMensualProjection row : dashboardRepository.sumarIngresosMensualesConfirmados(
                codper, desdeInclusive, hastaExclusive)) {
            result.computeIfAbsent(row.getCodprop(), ignored -> new HashMap<>())
                    .put(YearMonth.of(row.getAnio(), row.getMes()), money(row.getTotal()));
        }
        return result;
    }

    private List<DashboardIngresoMensualResponse> monthlySeries(
            List<YearMonth> periodos, Map<YearMonth, BigDecimal> montos) {
        return periodos.stream()
                .map(periodo -> new DashboardIngresoMensualResponse(periodo.toString(),
                        money(montos.getOrDefault(periodo, ZERO_MONEY))))
                .toList();
    }

    private BigDecimal percentage(BigDecimal ingresos, BigDecimal inversion) {
        if (inversion.signum() == 0) {
            return null;
        }
        return ingresos.multiply(ONE_HUNDRED).divide(inversion, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO_MONEY : value.setScale(2, RoundingMode.HALF_UP);
    }
}
