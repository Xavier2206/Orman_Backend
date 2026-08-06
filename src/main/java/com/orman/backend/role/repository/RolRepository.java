package com.orman.backend.role.repository;

import com.orman.backend.role.entity.Rol;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndCodrNot(String nombre, Integer codr);

    Page<Rol> findAllByOrderByNombreAsc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rol r where r.nombre = :nombre")
    Optional<Rol> findByNombreForUpdate(@Param("nombre") String nombre);
}
