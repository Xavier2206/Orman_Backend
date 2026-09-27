package com.orman.backend.payment.repository;

import com.orman.backend.payment.entity.QrCobroEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QrCobroRepository extends JpaRepository<QrCobroEntity, Integer> {

    @EntityGraph(attributePaths = "propietaria")
    List<QrCobroEntity> findAllByPropietariaCodperOrderByFechaInicioDescCodqrDesc(Integer codperPropietaria);

    @EntityGraph(attributePaths = "propietaria")
    Optional<QrCobroEntity> findByCodqrAndPropietariaCodper(Integer codqr, Integer codperPropietaria);

    @EntityGraph(attributePaths = "propietaria")
    @Query("""
            select qr from QrCobroEntity qr
             where qr.propietaria.codper = :codper
               and qr.estado = com.orman.backend.payment.entity.QrCobroEstado.ACTIVO
               and qr.fechaInicio <= :fecha
               and qr.fechaFin >= :fecha
             order by qr.fechaInicio desc, qr.codqr desc
            """)
    List<QrCobroEntity> findActiveForDate(@Param("codper") Integer codper, @Param("fecha") LocalDate fecha);

    @Query("""
            select (count(qr) > 0) from QrCobroEntity qr
             where qr.propietaria.codper = :codper
               and qr.estado = com.orman.backend.payment.entity.QrCobroEstado.ACTIVO
               and qr.fechaInicio <= :fechaFin
               and qr.fechaFin >= :fechaInicio
               and (:excludeCodqr is null or qr.codqr <> :excludeCodqr)
            """)
    boolean existsActiveOverlap(@Param("codper") Integer codper, @Param("fechaInicio") LocalDate fechaInicio,
                                @Param("fechaFin") LocalDate fechaFin, @Param("excludeCodqr") Integer excludeCodqr);
}
