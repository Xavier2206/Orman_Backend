package com.orman.backend.process.repository;

import com.orman.backend.process.entity.Proceso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcesoRepository extends JpaRepository<Proceso, Integer> {

    boolean existsByNombre(String nombre);

    boolean existsByEnlace(String enlace);
}
