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

    @Query("""
            select (count(ru) > 0)
            from RolUsu ru
            where ru.id.login = :login
              and ru.rol.nombre = 'PROPIETARIO'
              and ru.rol.estado = 1
            """)
    boolean existsActiveOwnerRoleByLogin(@Param("login") String login);

    @Query("""
            select (count(ru) > 0)
            from RolUsu ru
            where ru.usuario.persona.codper = :codper
              and ru.rol.nombre = 'PROPIETARIO'
              and ru.rol.estado = 1
            """)
    boolean existsActiveOwnerRoleByPerson(@Param("codper") Integer codper);

    @Query("""
            select (count(ru) > 0)
            from RolUsu ru
            where ru.id.login = :login
              and ru.rol.nombre = 'PROPIETARIO'
              and ru.rol.estado = 1
              and ru.usuario.estado = 1
              and ru.usuario.persona.estado = 1
            """)
    boolean existsActiveOwnerByLogin(@Param("login") String login);

    @Query("""
            select (count(ru) > 0)
            from RolUsu ru
            where ru.usuario.persona.codper = :codper
              and ru.rol.nombre = 'PROPIETARIO'
              and ru.rol.estado = 1
              and ru.usuario.estado = 1
              and ru.usuario.persona.estado = 1
            """)
    boolean existsActiveOwnerByPerson(@Param("codper") Integer codper);

    @Query("""
            select count(ru)
            from RolUsu ru
            where ru.rol.nombre = 'PROPIETARIO'
              and ru.rol.estado = 1
              and ru.usuario.estado = 1
              and ru.usuario.persona.estado = 1
            """)
    long countActiveOwners();

    List<RolUsu> findByIdLoginOrderByFechaAsignacionAsc(String login);

    List<RolUsu> findByIdCodrOrderByFechaAsignacionAsc(Integer codr);
}
