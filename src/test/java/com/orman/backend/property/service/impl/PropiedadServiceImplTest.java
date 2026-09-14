package com.orman.backend.property.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.dto.response.PropiedadResumenResponse;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.mapper.PropiedadMapper;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.PropiedadResumenProjection;
import com.orman.backend.property.repository.PropiedadUnidadCountProjection;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropiedadServiceImplTest {

    @Mock private PropiedadRepository propiedadRepository;
    @Mock private UnidadRepository unidadRepository;
    @Mock private PropertyOwnershipService propertyOwnershipService;
    @Mock private PropiedadResumenProjection projection;
    @Mock private PropiedadMapper propiedadMapper;
    @Mock private Authentication authentication;
    @Mock private PropiedadUnidadCountProjection unitMetrics;
    @InjectMocks private PropiedadServiceImpl service;

    @Test
    void calculatesGlobalOccupancyWithTheAuthenticatedOwnerAndTwoDecimals() {
        Persona propietaria = new Persona();
        ReflectionTestUtils.setField(propietaria, "codper", 7);
        when(propertyOwnershipService.currentPropietaria(authentication)).thenReturn(propietaria);
        when(propiedadRepository.findResumenByPropietaria(7)).thenReturn(projection);
        when(projection.getInversionTotal()).thenReturn(new BigDecimal("3500.00"));
        when(projection.getPropiedadesActivas()).thenReturn(2L);
        when(projection.getCasasActivas()).thenReturn(1L);
        when(projection.getEdificiosActivos()).thenReturn(1L);
        when(projection.getUnidadesTotales()).thenReturn(5L);
        when(projection.getUnidadesHabilitadas()).thenReturn(3L);
        when(projection.getUnidadesNoHabilitadas()).thenReturn(2L);
        when(projection.getUnidadesOcupadas()).thenReturn(1L);

        PropiedadResumenResponse response = service.resumen(authentication);

        assertThat(response.inversionTotal()).isEqualByComparingTo("3500.00");
        assertThat(response.propiedadesActivas()).isEqualTo(2);
        assertThat(response.casasActivas()).isEqualTo(1);
        assertThat(response.edificiosActivos()).isEqualTo(1);
        assertThat(response.unidadesTotales()).isEqualTo(5);
        assertThat(response.unidadesHabilitadas()).isEqualTo(3);
        assertThat(response.unidadesNoHabilitadas()).isEqualTo(2);
        assertThat(response.unidadesOcupadas()).isEqualTo(1);
        assertThat(response.ocupacionGlobal()).isEqualByComparingTo("33.33");
        verify(propiedadRepository).findResumenByPropietaria(7);
    }

    @Test
    void returnsZeroOccupancyWhenThereAreNoEnabledUnits() {
        Persona propietaria = new Persona();
        ReflectionTestUtils.setField(propietaria, "codper", 8);
        when(propertyOwnershipService.currentPropietaria(authentication)).thenReturn(propietaria);
        when(propiedadRepository.findResumenByPropietaria(8)).thenReturn(projection);
        when(projection.getInversionTotal()).thenReturn(BigDecimal.ZERO.setScale(2));
        when(projection.getUnidadesHabilitadas()).thenReturn(0L);

        PropiedadResumenResponse response = service.resumen(authentication);

        assertThat(response.ocupacionGlobal()).isEqualByComparingTo("0.00");
        verify(propiedadRepository).findResumenByPropietaria(8);
    }

    @Test
    void enrichesThePropertyPageWithAllMetricsUsingOneAggregateQuery() {
        Persona propietaria = new Persona();
        ReflectionTestUtils.setField(propietaria, "codper", 7);
        PropiedadEntity propiedad = new PropiedadEntity();
        ReflectionTestUtils.setField(propiedad, "codprop", 161);
        Pageable pageable = PageRequest.of(0, 20);
        when(propertyOwnershipService.currentPropietaria(authentication)).thenReturn(propietaria);
        when(propiedadRepository.searchOwned(eq(7), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(propiedad), pageable, 1));
        when(unitMetrics.getCodprop()).thenReturn(161);
        when(unitMetrics.getCantidadUnidades()).thenReturn(9L);
        when(unitMetrics.getUnidadesHabilitadas()).thenReturn(8L);
        when(unitMetrics.getUnidadesOcupadas()).thenReturn(7L);
        when(unidadRepository.countByPropiedadesOwned(anyCollection(), eq(7))).thenReturn(List.of(unitMetrics));
        PropiedadResponse mapped = new PropiedadResponse(161, "Edificio Tarija", "EDIFICIO", "Calle 1", "Tarija",
                null, null, null, null, 7, new BigDecimal("3500000.00"), (short) 1, 9, 8, 7,
                new BigDecimal("87.50"));
        when(propiedadMapper.toResponse(propiedad, 9L, 8L, 7L, new BigDecimal("87.50"))).thenReturn(mapped);

        PageResponse<PropiedadResponse> result = service.list(null, null, null, pageable, authentication);

        assertThat(result.content()).containsExactly(mapped);
        assertThat(result.content().getFirst().ocupacion()).isEqualByComparingTo("87.50");
        verify(unidadRepository, times(1)).countByPropiedadesOwned(anyCollection(), eq(7));
        verify(unidadRepository, never()).countByPropiedadCodpropAndPropiedadPropietariaCodper(any(), any());
    }
}
