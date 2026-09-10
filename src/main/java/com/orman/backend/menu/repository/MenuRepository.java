package com.orman.backend.menu.repository;

import com.orman.backend.menu.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuRepository extends JpaRepository<Menu, Integer> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndCodmNot(String nombre, Integer codm);

    long countByEstado(Short estado);

    @Query(value = """
            select m from Menu m
            where (:q is null or lower(m.nombre) like lower(concat('%', cast(:q as string), '%')))
              and (:estado is null or m.estado = :estado)
            """,
            countQuery = """
            select count(m) from Menu m
            where (:q is null or lower(m.nombre) like lower(concat('%', cast(:q as string), '%')))
              and (:estado is null or m.estado = :estado)
            """)
    Page<Menu> search(@Param("q") String q, @Param("estado") Short estado, Pageable pageable);
}
