package com.orman.backend.contract.service.impl;

import com.orman.backend.contract.dto.request.CuotaListCriteria;
import com.orman.backend.contract.repository.CuotaListPage;
import com.orman.backend.contract.repository.CuotaListProjection;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.mapper.CuotaMapper;
import com.orman.backend.contract.service.ContractOwnershipService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaServiceGlobalListTest {

    @Mock private CuotaRepository cuotaRepository;
    @Mock private CuotaMapper cuotaMapper;
    @Mock private ContractOwnershipService contractOwnershipService;
    @Mock private PropertyOwnershipService propertyOwnershipService;
    @Mock private Authentication authentication;
    @Mock private Persona owner;

    @Test
    void derivesAllDueSituationsWithLaPazClockAndUsesOneRepositoryOperation() {
        LocalDate today = LocalDate.of(2026, 9, 24);
        LocalDate deadline = today.plusDays(7);
        Clock clock = Clock.fixed(Instant.parse("2026-09-25T02:00:00Z"), ZoneOffset.UTC);
        CuotaListCriteria criteria = CuotaListCriteria.from(null, null, null, null, null, null,
                null, null, null);
        List<CuotaListProjection> rows = List.of(
                row(1, LocalDate.of(2026, 9, 23), "100.00", "0.00", "100.00"),
                row(2, today, "100.00", "0.00", "100.00"),
                row(3, today.plusDays(1), "100.00", "0.00", "100.00"),
                row(4, deadline, "100.00", "0.00", "100.00"),
                row(5, deadline.plusDays(1), "100.00", "0.00", "100.00"),
                row(6, LocalDate.of(2026, 8, 1), "100.00", "100.00", "0.00"));

        when(propertyOwnershipService.currentPropietaria(authentication)).thenReturn(owner);
        when(owner.getCodper()).thenReturn(42);
        when(cuotaRepository.searchOwned(42, criteria, today, deadline))
                .thenReturn(new CuotaListPage(rows, rows.size()));

        CuotaServiceImpl service = new CuotaServiceImpl(cuotaRepository, cuotaMapper,
                contractOwnershipService, propertyOwnershipService, clock);
        var response = service.listGlobal(criteria, authentication);

        assertThat(response.content()).extracting(item -> item.situacionVencimiento())
                .containsExactly("VENCIDA", "HOY", "PROXIMA", "PROXIMA", "AL_DIA", "SIN_SALDO");
        assertThat(response.totalElements()).isEqualTo(6);
        verify(cuotaRepository).searchOwned(42, criteria, today, deadline);
        verifyNoMoreInteractions(cuotaRepository);
        verifyNoInteractions(cuotaMapper, contractOwnershipService);
    }

    private CuotaListProjection row(Integer codcuo, LocalDate due, String amount,
                                    String confirmed, String balance) {
        BigDecimal monto = new BigDecimal(amount);
        BigDecimal montoConfirmado = new BigDecimal(confirmed);
        return new CuotaListProjection(codcuo, 100, due.withDayOfMonth(1), due, 200, "Inquilino Prueba", "123",
                300, "Propiedad Prueba", 400, "Unidad Prueba", monto, montoConfirmado, BigDecimal.ZERO,
                new BigDecimal(balance), "PENDIENTE");
    }
}
