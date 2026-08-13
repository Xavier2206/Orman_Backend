package com.orman.backend.user.repository;

import com.orman.backend.user.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    boolean existsByPersonaCodper(Integer codper);

    Optional<Usuario> findByPersonaCodper(Integer codper);

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
}
