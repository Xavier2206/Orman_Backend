package com.orman.backend.role.repository;

import com.orman.backend.role.entity.RolUsu;
import com.orman.backend.role.entity.RolUsuId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolUsuRepository extends JpaRepository<RolUsu, RolUsuId> {

    List<RolUsu> findByIdLoginOrderByFechaAsignacionAsc(String login);

    List<RolUsu> findByIdCodrOrderByFechaAsignacionAsc(Integer codr);
}
