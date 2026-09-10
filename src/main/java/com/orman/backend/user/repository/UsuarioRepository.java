package com.orman.backend.user.repository;

import com.orman.backend.user.entity.Usuario;
import com.orman.backend.person.dto.PersonaUsuarioRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    @Override
    @EntityGraph(attributePaths = "persona")
    Optional<Usuario> findById(String login);

    boolean existsByPersonaCodper(Integer codper);

    Optional<Usuario> findByPersonaCodper(Integer codper);

    List<Usuario> findByPersonaCodperIn(Collection<Integer> codpers);

    @Query("""
            select new com.orman.backend.person.dto.PersonaUsuarioRow(u.persona.codper, u.login, u.estado)
            from Usuario u where u.persona.codper in :codpers
            """)
    List<PersonaUsuarioRow> findSummariesByPersonaCodperIn(@Param("codpers") Collection<Integer> codpers);

    @EntityGraph(attributePaths = "persona")
    @Query("select u from Usuario u where u.login = :login")
    Optional<Usuario> findByLoginWithPersona(@Param("login") String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "persona")
    @Query("select u from Usuario u where u.login = :login")
    Optional<Usuario> findByLoginForUpdate(@Param("login") String login);

    @Query("""
            select u
            from Usuario u
            where not exists (
                select ru.id.login
                from RolUsu ru
                where ru.usuario = u
                  and ru.rol.nombre = 'PROPIETARIO'
                  and ru.rol.estado = 1
            )
            """)
    Page<Usuario> findAllCommon(Pageable pageable);

    @Query(value = """
            select u
            from Usuario u
            where lower(u.login) like lower(concat('%', cast(:q as string), '%'))
               or lower(u.persona.nombre) like lower(concat('%', cast(:q as string), '%'))
               or lower(coalesce(u.persona.ap, '')) like lower(concat('%', cast(:q as string), '%'))
               or lower(coalesce(u.persona.am, '')) like lower(concat('%', cast(:q as string), '%'))
            """,
            countQuery = """
            select count(u)
            from Usuario u
            where lower(u.login) like lower(concat('%', cast(:q as string), '%'))
               or lower(u.persona.nombre) like lower(concat('%', cast(:q as string), '%'))
               or lower(coalesce(u.persona.ap, '')) like lower(concat('%', cast(:q as string), '%'))
               or lower(coalesce(u.persona.am, '')) like lower(concat('%', cast(:q as string), '%'))
            """)
    Page<Usuario> search(@Param("q") String q, Pageable pageable);

    @Query(value = """
            select u
            from Usuario u
            where (
                   lower(u.login) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.persona.nombre) like lower(concat('%', cast(:q as string), '%'))
                or lower(coalesce(u.persona.ap, '')) like lower(concat('%', cast(:q as string), '%'))
                or lower(coalesce(u.persona.am, '')) like lower(concat('%', cast(:q as string), '%'))
            )
              and not exists (
                select ru.id.login
                from RolUsu ru
                where ru.usuario = u
                  and ru.rol.nombre = 'PROPIETARIO'
                  and ru.rol.estado = 1
              )
            """,
            countQuery = """
            select count(u)
            from Usuario u
            where (
                   lower(u.login) like lower(concat('%', cast(:q as string), '%'))
                or lower(u.persona.nombre) like lower(concat('%', cast(:q as string), '%'))
                or lower(coalesce(u.persona.ap, '')) like lower(concat('%', cast(:q as string), '%'))
                or lower(coalesce(u.persona.am, '')) like lower(concat('%', cast(:q as string), '%'))
            )
              and not exists (
                select ru.id.login
                from RolUsu ru
                where ru.usuario = u
                  and ru.rol.nombre = 'PROPIETARIO'
                  and ru.rol.estado = 1
              )
            """)
    Page<Usuario> searchCommon(@Param("q") String q, Pageable pageable);
}
