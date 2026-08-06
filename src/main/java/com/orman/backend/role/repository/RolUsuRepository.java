package com.orman.backend.role.repository;

import com.orman.backend.role.entity.RolUsu;
import com.orman.backend.role.entity.RolUsuId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolUsuRepository extends JpaRepository<RolUsu, RolUsuId> {

    @Query("""
            select distinct r.nombre
            from RolUsu ru
            join ru.rol r
            where ru.id.login = :login
              and r.estado = 1
            order by r.nombre
            """)
    List<String> findActiveRoleNamesByLogin(@Param("login") String login);

    List<RolUsu> findByIdLoginOrderByFechaAsignacionAsc(String login);

    List<RolUsu> findByIdCodrOrderByFechaAsignacionAsc(Integer codr);
}
