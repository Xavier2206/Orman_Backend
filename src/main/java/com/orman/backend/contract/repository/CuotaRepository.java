package com.orman.backend.contract.repository;

import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuotaRepository extends JpaRepository<CuotaEntity, Integer> {

    boolean existsByContratoCodconAndPeriodo(Integer codcon, LocalDate periodo);

    List<CuotaEntity> findAllByContratoCodconOrderByPeriodoAsc(Integer codcon);

    @Query("""
            select (count(c) > 0) from CuotaEntity c
            where c.contrato.codcon = :codcon
              and c.periodo <= :periodo
              and c.estado <> com.orman.backend.contract.entity.CuotaEstado.PAGADA
            """)
    boolean existsUnpaidThroughPeriod(@Param("codcon") Integer codcon, @Param("periodo") LocalDate periodo);

    @Query("""
            select (count(c) > 0) from CuotaEntity c
            where c.contrato.codcon = :codcon
              and c.estado in :estados
            """)
    boolean existsByContratoAndEstados(@Param("codcon") Integer codcon,
                                       @Param("estados") Collection<CuotaEstado> estados);

    @Query(value = """
            select exists (
                select 1
                  from pagos p
                  join cuotas q on q.codcuo = p.codcuo
                 where q.codcon = :codcon
                   and p.estado = 'PENDIENTE_REVISION'
            )
            """, nativeQuery = true)
    boolean existsPendingReviewPaymentByContrato(@Param("codcon") Integer codcon);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CuotaEntity c
               set c.estado = com.orman.backend.contract.entity.CuotaEstado.ANULADA
             where c.contrato.codcon = :codcon
               and c.periodo > :periodo
            """)
    int annulAfterPeriod(@Param("codcon") Integer codcon, @Param("periodo") LocalDate periodo);

    @Query(value = """
            select coalesce(sum(p.monto), 0)
              from pagos p
             where p.codcuo = :codcuo
               and p.estado = :estado
            """, nativeQuery = true)
    java.math.BigDecimal sumPaymentAmountByState(@Param("codcuo") Integer codcuo,
                                                  @Param("estado") String estado);

    @EntityGraph(attributePaths = {"contrato", "contrato.inquilino", "contrato.unidad", "contrato.unidad.propiedad",
            "contrato.unidad.propiedad.propietaria"})
    Optional<CuotaEntity> findByCodcuo(Integer codcuo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"contrato", "contrato.inquilino", "contrato.unidad", "contrato.unidad.propiedad",
            "contrato.unidad.propiedad.propietaria"})
    @Query("select c from CuotaEntity c where c.codcuo = :codcuo")
    Optional<CuotaEntity> findByCodcuoForUpdate(@Param("codcuo") Integer codcuo);

    @EntityGraph(attributePaths = {"contrato", "contrato.unidad", "contrato.unidad.propiedad",
            "contrato.unidad.propiedad.propietaria"})
    @Query("""
            select c from CuotaEntity c
            where c.fechaVencimiento <= :fechaLimite
              and c.estado in :estados
            order by c.fechaVencimiento asc, c.codcuo asc
            """)
    List<CuotaEntity> findAllPendingOrPartialDueOnOrBefore(@Param("fechaLimite") LocalDate fechaLimite,
                                                            @Param("estados") Collection<CuotaEstado> estados);

    @Query(value = """
            select q.codcon as codcon,
                   count(q.codcuo) as "totalCuotas",
                   count(case when q.estado = 'PAGADA' then 1 end) as "cuotasPagadas",
                   count(case when q.estado in ('PENDIENTE', 'PARCIAL') then 1 end) as "cuotasPendientes",
                   coalesce(sum(
                       case
                           when q.estado = 'PAGADA' then 0
                           when q.estado = 'PARCIAL' then q.monto - coalesce(p.monto_pagado, 0)
                           else q.monto
                       end
                   ), 0) as "saldoPendiente"
              from cuotas q
              left join (
                  select p.codcuo, sum(p.monto) as monto_pagado
                    from pagos p
                   where p.estado = 'CONFIRMADO'
                   group by p.codcuo
              ) p on p.codcuo = q.codcuo
             where q.codcon in (:codcons)
               and q.estado <> 'ANULADA'
             group by q.codcon
            """, nativeQuery = true)
    List<ContratoCuotasResumenProjection> summarizeByContratoCodcons(@Param("codcons") Collection<Integer> codcons);
}
