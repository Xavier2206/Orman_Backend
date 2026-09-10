package com.orman.backend.contract.repository;

import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratoRepository extends JpaRepository<ContratoEntity, Integer> {

    @EntityGraph(attributePaths = {"unidad", "unidad.propiedad", "unidad.propiedad.propietaria", "inquilino", "contratoOrigen"})
    Optional<ContratoEntity> findByCodcon(Integer codcon);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"unidad", "unidad.propiedad", "unidad.propiedad.propietaria", "inquilino", "contratoOrigen"})
    @Query("select c from ContratoEntity c where c.codcon = :codcon")
    Optional<ContratoEntity> findByCodconForUpdate(@Param("codcon") Integer codcon);

    @EntityGraph(attributePaths = {"unidad", "unidad.propiedad", "unidad.propiedad.propietaria", "inquilino", "contratoOrigen"})
    @Query("""
            select c from ContratoEntity c
            where c.unidad.coduni = :coduni
              and c.unidad.propiedad.propietaria.codper = :codper
            """)
    Page<ContratoEntity> findAllByUnidadOwned(@Param("coduni") Integer coduni, @Param("codper") Integer codper,
                                              Pageable pageable);

    @EntityGraph(attributePaths = {"unidad", "unidad.propiedad", "unidad.propiedad.propietaria", "inquilino", "contratoOrigen"})
    @Query(value = """
            select c from ContratoEntity c
            where c.unidad.propiedad.propietaria.codper = :codper
              and (:coduni is null or c.unidad.coduni = :coduni)
              and (:estado is null or c.estado = :estado)
            """,
            countQuery = """
            select count(c) from ContratoEntity c
            where c.unidad.propiedad.propietaria.codper = :codper
              and (:coduni is null or c.unidad.coduni = :coduni)
              and (:estado is null or c.estado = :estado)
            """)
    Page<ContratoEntity> searchOwned(@Param("codper") Integer codper, @Param("coduni") Integer coduni,
                                     @Param("estado") ContratoEstado estado, Pageable pageable);

    boolean existsByUnidadCoduniAndEstado(Integer coduni, ContratoEstado estado);
}
