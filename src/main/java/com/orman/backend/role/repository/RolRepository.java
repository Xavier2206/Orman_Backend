package com.orman.backend.role.repository;

import com.orman.backend.role.entity.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndCodrNot(String nombre, Integer codr);

    Page<Rol> findAllByOrderByNombreAsc(Pageable pageable);
}
