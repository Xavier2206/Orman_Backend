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

    @Query(value = """
            select
                (select coalesce(sum(p2.inversion_inicial), 0)
                   from propiedades p2
                  where p2.codper_propietaria = :codper) as "inversionTotal",
                count(distinct p.codprop) filter (where p.estado = 1) as "propiedadesActivas",
                count(distinct p.codprop) filter (where p.estado = 1 and p.tipo = 'CASA') as "casasActivas",
                count(distinct p.codprop) filter (where p.estado = 1 and p.tipo = 'EDIFICIO') as "edificiosActivos",
                count(u.coduni) as "unidadesTotales",
                count(u.coduni) filter (where u.estado_operativo = 1) as "unidadesHabilitadas",
                count(u.coduni) filter (where u.estado_operativo = 0) as "unidadesNoHabilitadas",
                count(u.coduni) filter (where u.estado_operativo = 1 and c.estado = 'VIGENTE') as "unidadesOcupadas"
            from propiedades p
            left join unidades u on u.codprop = p.codprop
            left join contratos c on c.coduni = u.coduni and c.estado = 'VIGENTE'
            where p.codper_propietaria = :codper
            """, nativeQuery = true)
    PropiedadResumenProjection findResumenByPropietaria(@Param("codper") Integer codper);

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
