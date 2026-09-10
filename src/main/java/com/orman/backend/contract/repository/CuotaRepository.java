package com.orman.backend.contract.repository;

import com.orman.backend.contract.entity.CuotaEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuotaRepository extends JpaRepository<CuotaEntity, Integer> {

    boolean existsByContratoCodconAndPeriodo(Integer codcon, LocalDate periodo);

    List<CuotaEntity> findAllByContratoCodconOrderByPeriodoAsc(Integer codcon);
}
