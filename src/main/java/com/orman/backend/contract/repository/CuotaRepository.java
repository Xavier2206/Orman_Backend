package com.orman.backend.contract.repository;

import com.orman.backend.contract.entity.CuotaEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuotaRepository extends JpaRepository<CuotaEntity, Integer> {

    boolean existsByContratoCodconAndPeriodo(Integer codcon, LocalDate periodo);

    List<CuotaEntity> findAllByContratoCodconOrderByPeriodoAsc(Integer codcon);

    @EntityGraph(attributePaths = {"contrato", "contrato.unidad", "contrato.unidad.propiedad",
            "contrato.unidad.propiedad.propietaria"})
    Optional<CuotaEntity> findByCodcuo(Integer codcuo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"contrato", "contrato.unidad", "contrato.unidad.propiedad",
            "contrato.unidad.propiedad.propietaria"})
    @Query("select c from CuotaEntity c where c.codcuo = :codcuo")
    Optional<CuotaEntity> findByCodcuoForUpdate(@Param("codcuo") Integer codcuo);
}
