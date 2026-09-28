package com.orman.backend.contract.dto.request;

import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.exception.InvalidCuotaListFilterException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuotaListCriteriaTest {

    @Test
    void appliesPaginationDefaultsAndParsesDefinedFilters() {
        CuotaListCriteria criteria = CuotaListCriteria.from("9", "2026-09-01", "parcial", "proximas",
                "2", "4", "true", null, null);

        assertThat(criteria.codperInquilino()).isEqualTo(9);
        assertThat(criteria.codcuo()).isNull();
        assertThat(criteria.periodo()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(criteria.estado()).isEqualTo(CuotaEstado.PARCIAL);
        assertThat(criteria.vencimiento()).isEqualTo(CuotaListCriteria.Vencimiento.PROXIMAS);
        assertThat(criteria.codprop()).isEqualTo(2);
        assertThat(criteria.coduni()).isEqualTo(4);
        assertThat(criteria.conPagoPendienteRevision()).isTrue();
        assertThat(criteria.page()).isZero();
        assertThat(criteria.size()).isEqualTo(20);
    }

    @Test
    void parsesOptionalQuotaIdAsPositiveInteger() {
        CuotaListCriteria criteria = CuotaListCriteria.from(null, "1894", null, null, null,
                null, null, null, null, null);

        assertThat(criteria.codcuo()).isEqualTo(1894);
        assertInvalid("codcuo", () -> CuotaListCriteria.from(null, "0", null, null, null,
                null, null, null, null, null));
        assertInvalid("codcuo", () -> CuotaListCriteria.from(null, "abc", null, null, null,
                null, null, null, null, null));
    }

    @Test
    void acceptsMaximumPageSizeAndRejectsInvalidValues() {
        assertThat(CuotaListCriteria.from(null, null, null, null, null, null, null, "2", "100").size())
                .isEqualTo(100);

        assertInvalid("page", () -> criteria("-1", null, null, null));
        assertInvalid("size", () -> criteria(null, "0", null, null));
        assertInvalid("size", () -> criteria(null, "101", null, null));
        assertInvalid("codprop", () -> CuotaListCriteria.from(null, null, null, null, "0", null,
                null, null, null));
        assertInvalid("periodo", () -> CuotaListCriteria.from(null, "2026-09-02", null, null, null,
                null, null, null, null));
        assertInvalid("estado", () -> CuotaListCriteria.from(null, null, "VENCIDA", null, null,
                null, null, null, null));
        assertInvalid("vencimiento", () -> CuotaListCriteria.from(null, null, null, "PAGADA", null,
                null, null, null, null));
        assertInvalid("conPagoPendienteRevision", () -> CuotaListCriteria.from(null, null, null, null,
                null, null, "yes", null, null));
    }

    private CuotaListCriteria criteria(String page, String size, String state, String due) {
        return CuotaListCriteria.from(null, null, state, due, null, null, null, page, size);
    }

    private void assertInvalid(String expectedField, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(InvalidCuotaListFilterException.class)
                .extracting("field").isEqualTo(expectedField);
    }
}
