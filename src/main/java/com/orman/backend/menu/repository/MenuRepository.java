package com.orman.backend.menu.repository;

import com.orman.backend.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Integer> {

    boolean existsByNombre(String nombre);
}
