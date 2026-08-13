package com.orman.backend.auth.repository;

import com.orman.backend.role.entity.RolUsu;
import com.orman.backend.role.entity.RolUsuId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthContextRepository extends JpaRepository<RolUsu, RolUsuId> {

    @Query("""
            select new com.orman.backend.auth.repository.AuthContextNavigationRow(
                    r.codr, r.nombre, m.codm, m.nombre, m.icono, p.codp, p.nombre, p.enlace)
            from RolUsu ru
            join ru.rol r
            left join RolMe rm on rm.rol = r and rm.menu.estado = 1
            left join rm.menu m
            left join MePro mp on mp.menu = m and mp.proceso.estado = 1
            left join mp.proceso p
            where ru.id.login = :login
              and r.estado = 1
            order by r.nombre asc, r.codr asc, m.nombre asc, m.codm asc, p.nombre asc, p.codp asc
            """)
    List<AuthContextNavigationRow> findActiveNavigationByLogin(@Param("login") String login);
}
