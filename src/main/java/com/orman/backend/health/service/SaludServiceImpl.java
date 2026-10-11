package com.orman.backend.health.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SaludServiceImpl implements SaludService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean comprobarBaseDatos() {
        try {
            return Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class));
        } catch (DataAccessException exception) {
            return false;
        }
    }
}
