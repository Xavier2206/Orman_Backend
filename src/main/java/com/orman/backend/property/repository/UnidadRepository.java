package com.orman.backend.property.repository;

import com.orman.backend.property.entity.UnidadEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnidadRepository extends JpaRepository<UnidadEntity, Integer> {

    boolean existsByPropiedadCodpropAndNombre(Integer codprop, String nombre);

    boolean existsByPropiedadCodpropAndNombreAndCoduniNot(Integer codprop, String nombre, Integer coduni);

    long countByPropiedadCodpropAndPropiedadPropietariaCodper(Integer codprop, Integer codper);

    @Query(value = """
            select u.codprop as codprop,
                   count(*) as "cantidadUnidades",
                   count(*) filter (where u.estado_operativo = 1) as "unidadesHabilitadas",
                   count(*) filter (where u.estado_operativo = 1 and exists (
                       select 1
                         from contratos c
                        where c.coduni = u.coduni
                          and c.estado = 'VIGENTE'
                          and CURRENT_DATE >= c.fecha_inicio
                          and CURRENT_DATE < c.fecha_fin
                   )) as "unidadesOcupadas"
              from unidades u
              join propiedades p on p.codprop = u.codprop
             where u.codprop in (:codprops)
               and p.codper_propietaria = :codper
             group by u.codprop
            """, nativeQuery = true)
    List<PropiedadUnidadCountProjection> countByPropiedadesOwned(@Param("codprops") Collection<Integer> codprops,
                                                                  @Param("codper") Integer codper);

    @EntityGraph(attributePaths = {"propiedad", "propiedad.propietaria"})
    Optional<UnidadEntity> findByCoduniAndPropiedadPropietariaCodper(Integer coduni, Integer codper);

    @EntityGraph(attributePaths = {"propiedad", "propiedad.propietaria"})
    Optional<UnidadEntity> findByCoduni(Integer coduni);

    @EntityGraph(attributePaths = "propiedad")
    @Query("""
            select u from UnidadEntity u
            where u.propiedad.codprop = :codprop
              and u.propiedad.propietaria.codper = :codper
              and (:estadoOperativo is null or u.estadoOperativo = :estadoOperativo)
            """)
    Page<UnidadEntity> findAllByPropiedadOwned(@Param("codprop") Integer codprop, @Param("codper") Integer codper,
                                               @Param("estadoOperativo") Short estadoOperativo, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"propiedad", "propiedad.propietaria"})
    @Query("""
            select u from UnidadEntity u
            where u.coduni = :coduni
              and u.propiedad.propietaria.codper = :codper
            """)
    Optional<UnidadEntity> findOwnedByCoduniForUpdate(@Param("coduni") Integer coduni,
                                                      @Param("codper") Integer codper);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"propiedad", "propiedad.propietaria"})
    @Query("select u from UnidadEntity u where u.coduni = :coduni")
    Optional<UnidadEntity> findByCoduniForUpdate(@Param("coduni") Integer coduni);
}
