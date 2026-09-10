package com.orman.backend.property.repository;

import com.orman.backend.property.entity.UnidadEntity;
import jakarta.persistence.LockModeType;
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

    @EntityGraph(attributePaths = {"propiedad", "propiedad.propietaria"})
    Optional<UnidadEntity> findByCoduniAndPropiedadPropietariaCodper(Integer coduni, Integer codper);

    @EntityGraph(attributePaths = {"propiedad", "propiedad.propietaria"})
    Optional<UnidadEntity> findByCoduni(Integer coduni);

    @EntityGraph(attributePaths = "propiedad")
    @Query("""
            select u from UnidadEntity u
            where u.propiedad.codprop = :codprop
              and u.propiedad.propietaria.codper = :codper
            """)
    Page<UnidadEntity> findAllByPropiedadOwned(@Param("codprop") Integer codprop, @Param("codper") Integer codper,
                                               Pageable pageable);

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
