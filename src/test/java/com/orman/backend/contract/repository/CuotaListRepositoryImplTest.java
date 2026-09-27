package com.orman.backend.contract.repository;

import com.orman.backend.contract.dto.request.CuotaListCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaListRepositoryImplTest {

    @Mock private EntityManager entityManager;
    @Mock private Query dataQuery;
    @Mock private Query countQuery;

    @Test
    void keepsDatabaseQueryCountFixedForALargePageAndOrdersBeforeLimit() {
        List<String> sql = new ArrayList<>();
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            sql.add(invocation.getArgument(0));
            return sql.size() == 1 ? dataQuery : countQuery;
        });
        when(dataQuery.getResultList()).thenReturn(rows(100));
        when(countQuery.getSingleResult()).thenReturn(100L);

        CuotaListRepository repository = new CuotaListRepositoryImpl(entityManager);
        CuotaListPage result = repository.searchOwned(77,
                CuotaListCriteria.from(null, null, null, null, null, null, null, "0", "100"),
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 2));

        assertThat(result.content()).hasSize(100);
        assertThat(result.content().getFirst().montoConfirmado()).isEqualByComparingTo("0.00");
        assertThat(result.totalElements()).isEqualTo(100);
        assertThat(sql).hasSize(2);
        assertThat(sql.getFirst()).contains("GROUP BY p.codcuo", "FILTER (WHERE p.estado = 'CONFIRMADO')",
                "FILTER (WHERE p.estado = 'PENDIENTE_REVISION')", "ORDER BY CASE",
                "LIMIT :limit OFFSET :offset");
        assertThat(sql.getFirst().indexOf("ORDER BY CASE"))
                .isLessThan(sql.getFirst().indexOf("LIMIT :limit OFFSET :offset"));
        verify(entityManager, times(2)).createNativeQuery(anyString());
    }

    private List<Object[]> rows(int count) {
        List<Object[]> rows = new ArrayList<>();
        LocalDate due = LocalDate.of(2026, 9, 1);
        for (int codcuo = 1; codcuo <= count; codcuo++) {
            rows.add(new Object[]{codcuo, 10, due, due, 20, "Inquilino Prueba", "123",
                    30, "Propiedad Prueba", 40, "Unidad Prueba", new BigDecimal("1500.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("1500.00"), "PENDIENTE"});
        }
        return rows;
    }
}
