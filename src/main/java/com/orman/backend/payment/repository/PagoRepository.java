package com.orman.backend.payment.repository;

import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<PagoEntity, Integer> {

    String OWNERSHIP_GRAPH = "cuota, cuota.contrato, cuota.contrato.unidad, cuota.contrato.unidad.propiedad, "
            + "cuota.contrato.unidad.propiedad.propietaria, cuentaPago";

    @EntityGraph(attributePaths = {"cuota", "cuota.contrato", "cuota.contrato.unidad", "cuota.contrato.unidad.propiedad",
            "cuota.contrato.unidad.propiedad.propietaria", "cuentaPago", "registradoPor", "revisadoPor"})
    Optional<PagoEntity> findByCodpag(Integer codpag);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"cuota", "cuota.contrato", "cuota.contrato.unidad", "cuota.contrato.unidad.propiedad",
            "cuota.contrato.unidad.propiedad.propietaria", "cuentaPago", "registradoPor", "revisadoPor"})
    @Query("select p from PagoEntity p where p.codpag = :codpag")
    Optional<PagoEntity> findByCodpagForUpdate(@Param("codpag") Integer codpag);

    @EntityGraph(attributePaths = {"cuota", "cuota.contrato", "cuota.contrato.unidad", "cuota.contrato.unidad.propiedad",
            "cuota.contrato.unidad.propiedad.propietaria", "cuentaPago", "registradoPor", "revisadoPor"})
    @Query("""
            select p from PagoEntity p
            where p.cuota.codcuo = :codcuo
              and p.cuota.contrato.unidad.propiedad.propietaria.codper = :codper
            order by p.fechaRegistro desc, p.codpag desc
            """)
    List<PagoEntity> findAllByCuotaOwned(@Param("codcuo") Integer codcuo, @Param("codper") Integer codper);

    @EntityGraph(attributePaths = {"cuota", "cuota.contrato", "cuota.contrato.unidad", "cuota.contrato.unidad.propiedad",
            "cuota.contrato.unidad.propiedad.propietaria", "cuentaPago", "registradoPor", "revisadoPor"})
    @Query(value = """
            select p from PagoEntity p
            where p.cuota.contrato.unidad.propiedad.propietaria.codper = :codper
              and (:estado is null or p.estado = :estado)
              and (:metodo is null or p.metodo = :metodo)
            """, countQuery = """
            select count(p) from PagoEntity p
            where p.cuota.contrato.unidad.propiedad.propietaria.codper = :codper
              and (:estado is null or p.estado = :estado)
              and (:metodo is null or p.metodo = :metodo)
            """)
    Page<PagoEntity> searchOwned(@Param("codper") Integer codper, @Param("estado") PagoEstado estado,
                                 @Param("metodo") MetodoPago metodo, Pageable pageable);

    Optional<PagoEntity> findByIdempotencyKey(UUID idempotencyKey);

    @Query("""
            select coalesce(sum(p.monto), 0)
            from PagoEntity p
            where p.cuota.codcuo = :codcuo and p.estado = com.orman.backend.payment.entity.PagoEstado.CONFIRMADO
            """)
    BigDecimal sumConfirmedMontoByCuota(@Param("codcuo") Integer codcuo);
}
