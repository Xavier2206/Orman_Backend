package com.orman.backend.contract.repository;

import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratoRepository extends JpaRepository<ContratoEntity, Integer> {

    @EntityGraph(attributePaths = {"unidad", "unidad.propiedad", "unidad.propiedad.propietaria", "inquilino"})
    Optional<ContratoEntity> findByCodcon(Integer codcon);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"unidad", "unidad.propiedad", "unidad.propiedad.propietaria", "inquilino"})
    @Query("select c from ContratoEntity c where c.codcon = :codcon")
    Optional<ContratoEntity> findByCodconForUpdate(@Param("codcon") Integer codcon);

    @EntityGraph(attributePaths = {"unidad", "unidad.propiedad", "unidad.propiedad.propietaria", "inquilino"})
    @Query("""
            select c from ContratoEntity c
            where c.unidad.coduni = :coduni
              and c.unidad.propiedad.propietaria.codper = :codper
            """)
    Page<ContratoEntity> findAllByUnidadOwned(@Param("coduni") Integer coduni, @Param("codper") Integer codper,
                                              Pageable pageable);

    @EntityGraph(attributePaths = {"unidad", "unidad.propiedad", "unidad.propiedad.propietaria", "inquilino"})
    @Query(value = """
            select c from ContratoEntity c
            where c.unidad.propiedad.propietaria.codper = :codper
              and (:codprop is null or c.unidad.propiedad.codprop = :codprop)
              and (:coduni is null or c.unidad.coduni = :coduni)
              and (:estado is null or c.estado = :estado)
              and (:q is null
                   or cast(function('translate', lower(c.inquilino.nombre), 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate', lower(coalesce(c.inquilino.ap, '')), 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate', lower(coalesce(c.inquilino.am, '')), 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate',
                                    lower(function('concat_ws', ' ',
                                                   c.inquilino.nombre,
                                                   nullif(c.inquilino.ap, ''),
                                                   nullif(c.inquilino.am, ''))),
                                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate',
                                    lower(function('concat_ws', ' ',
                                                   c.inquilino.nombre,
                                                   nullif(c.inquilino.am, ''))),
                                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate',
                                    lower(function('concat_ws', ' ',
                                                   nullif(c.inquilino.ap, ''),
                                                   nullif(c.inquilino.am, ''))),
                                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate',
                                    lower(function('concat_ws', ' ',
                                                   nullif(c.inquilino.ap, ''),
                                                   c.inquilino.nombre)),
                                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%')))
            """,
            countQuery = """
            select count(c) from ContratoEntity c
            where c.unidad.propiedad.propietaria.codper = :codper
              and (:codprop is null or c.unidad.propiedad.codprop = :codprop)
              and (:coduni is null or c.unidad.coduni = :coduni)
              and (:estado is null or c.estado = :estado)
              and (:q is null
                   or cast(function('translate', lower(c.inquilino.nombre), 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate', lower(coalesce(c.inquilino.ap, '')), 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate', lower(coalesce(c.inquilino.am, '')), 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate',
                                    lower(function('concat_ws', ' ',
                                                   c.inquilino.nombre,
                                                   nullif(c.inquilino.ap, ''),
                                                   nullif(c.inquilino.am, ''))),
                                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate',
                                    lower(function('concat_ws', ' ',
                                                   c.inquilino.nombre,
                                                   nullif(c.inquilino.am, ''))),
                                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate',
                                    lower(function('concat_ws', ' ',
                                                   nullif(c.inquilino.ap, ''),
                                                   nullif(c.inquilino.am, ''))),
                                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%'))
                   or cast(function('translate',
                                    lower(function('concat_ws', ' ',
                                                   nullif(c.inquilino.ap, ''),
                                                   c.inquilino.nombre)),
                                    'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') as string)
                       like lower(concat('%', cast(:q as string), '%')))
            """)
    Page<ContratoEntity> searchOwned(@Param("codper") Integer codper, @Param("codprop") Integer codprop,
                                     @Param("coduni") Integer coduni, @Param("estado") ContratoEstado estado,
                                     @Param("q") String q, Pageable pageable);

    boolean existsByUnidadCoduniAndEstadoIn(Integer coduni, Collection<ContratoEstado> estados);

    @Query("""
            select (count(c) > 0) from ContratoEntity c
            where c.unidad.coduni = :coduni
              and c.estado in (com.orman.backend.contract.entity.ContratoEstado.PROGRAMADO,
                               com.orman.backend.contract.entity.ContratoEstado.VIGENTE)
              and c.fechaInicio < :fechaFin
              and :fechaInicio < c.fechaFin
            """)
    boolean existsActiveOverlap(@Param("coduni") Integer coduni,
                                @Param("fechaInicio") LocalDate fechaInicio,
                                @Param("fechaFin") LocalDate fechaFin);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ContratoEntity c
               set c.estado = com.orman.backend.contract.entity.ContratoEstado.VIGENTE
             where c.estado = com.orman.backend.contract.entity.ContratoEstado.PROGRAMADO
               and c.fechaInicio <= :fechaActual
            """)
    int activateScheduled(@Param("fechaActual") LocalDate fechaActual);
}
