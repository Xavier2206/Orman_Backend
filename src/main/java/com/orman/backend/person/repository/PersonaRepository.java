package com.orman.backend.person.repository;

import com.orman.backend.person.entity.Persona;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Persona p where p.codper = :codper")
    Optional<Persona> findByCodperForUpdate(@Param("codper") Integer codper);

    boolean existsByCi(String ci);

    boolean existsByCiAndCodperNot(String ci, Integer codper);

    @Query("""
            select (count(c) > 0) from ContratoEntity c
            where c.inquilino.codper = :codper
              and c.inquilino.tipoPersona = 'I'
              and c.unidad.propiedad.propietaria.codper = :ownerCodper
            """)
    boolean existsTenantLinkedToOwner(@Param("codper") Integer codper,
                                      @Param("ownerCodper") Integer ownerCodper);

    @Query(value = """
            SELECT
                COUNT(p.codper) AS "totalPersonas",
                COUNT(*) FILTER (WHERE p.estado = 1) AS "activas",
                COUNT(*) FILTER (WHERE p.estado = 0) AS "inactivas",
                COUNT(u.login) AS "conUsuario"
            FROM personas p
            LEFT JOIN usuarios u ON u.codper = p.codper
            """, nativeQuery = true)
    PersonaResumenProjection findResumen();

    @Query(value = """
            select p from Persona p
            where (:q is null
                   or lower(p.nombre) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(p.ap, '')) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(p.am, '')) like lower(concat('%', cast(:q as string), '%'))
                   or lower(p.ci) like lower(concat('%', cast(:q as string), '%')))
              and (:tipoPersona is null or p.tipoPersona = :tipoPersona)
              and (:estado is null or p.estado = :estado)
            """,
            countQuery = """
            select count(p) from Persona p
            where (:q is null
                   or lower(p.nombre) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(p.ap, '')) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(p.am, '')) like lower(concat('%', cast(:q as string), '%'))
                   or lower(p.ci) like lower(concat('%', cast(:q as string), '%')))
              and (:tipoPersona is null or p.tipoPersona = :tipoPersona)
              and (:estado is null or p.estado = :estado)
            """)
    Page<Persona> search(@Param("q") String q, @Param("tipoPersona") Character tipoPersona,
                         @Param("estado") Short estado, Pageable pageable);
}
