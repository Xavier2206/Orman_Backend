package com.orman.backend.contract.repository;

public interface ContratoResumenProjection {

    long getVigentes();

    long getProgramados();

    long getFinalizados();

    long getRescindidos();
}
