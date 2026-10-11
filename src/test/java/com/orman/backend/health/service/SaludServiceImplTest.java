package com.orman.backend.health.service;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaludServiceImplTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final SaludServiceImpl saludService = new SaludServiceImpl(jdbcTemplate);

    @Test
    void indicaDisponibleCuandoLaConsultaDePostgresResponde() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        assertTrue(saludService.comprobarBaseDatos());
    }

    @Test
    void indicaNoDisponibleSinPropagarDetallesDelErrorDeBaseDeDatos() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("detalle interno de conexión"));

        assertFalse(saludService.comprobarBaseDatos());
    }
}
