package com.orman.backend.property.repository;

import com.orman.backend.property.entity.PropiedadEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropiedadRepository extends JpaRepository<PropiedadEntity, Integer> {

    @EntityGraph(attributePaths = "propietaria")
    Optional<PropiedadEntity> findByCodpropAndPropietariaCodper(Integer codprop, Integer codper);

    @EntityGraph(attributePaths = "propietaria")
    @Query(value = """
            select p from PropiedadEntity p
            where p.propietaria.codper = :codper
              and (:q is null
                   or lower(p.nombre) like lower(concat('%', cast(:q as string), '%'))
                   or lower(p.direccion) like lower(concat('%', cast(:q as string), '%'))
                   or lower(p.ciudad) like lower(concat('%', cast(:q as string), '%')))
              and (:tipo is null or p.tipo = :tipo)
              and (:estado is null or p.estado = :estado)
            """,
            countQuery = """
            select count(p) from PropiedadEntity p
            where p.propietaria.codper = :codper
              and (:q is null
                   or lower(p.nombre) like lower(concat('%', cast(:q as string), '%'))
                   or lower(p.direccion) like lower(concat('%', cast(:q as string), '%'))
                   or lower(p.ciudad) like lower(concat('%', cast(:q as string), '%')))
              and (:tipo is null or p.tipo = :tipo)
              and (:estado is null or p.estado = :estado)
            """)
    Page<PropiedadEntity> searchOwned(@Param("codper") Integer codper, @Param("q") String q,
                                      @Param("tipo") String tipo, @Param("estado") Short estado,
                                      Pageable pageable);
}
