package com.orman.backend.role.repository;

import com.orman.backend.role.entity.Rol;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    long countByEstado(Short estado);

    @Query(value = """
            select r from Rol r
            where (:q is null or lower(r.nombre) like lower(concat('%', cast(:q as string), '%')))
              and (:estado is null or r.estado = :estado)
            """,
            countQuery = """
            select count(r) from Rol r
            where (:q is null or lower(r.nombre) like lower(concat('%', cast(:q as string), '%')))
              and (:estado is null or r.estado = :estado)
            """)
    Page<Rol> search(@Param("q") String q, @Param("estado") Short estado, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rol r where r.nombre = :nombre")
    Optional<Rol> findByNombreForUpdate(@Param("nombre") String nombre);
}
