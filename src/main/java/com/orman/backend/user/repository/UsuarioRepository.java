package com.orman.backend.user.repository;

import com.orman.backend.user.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    boolean existsByPersonaCodper(Integer codper);

    Optional<Usuario> findByPersonaCodper(Integer codper);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "persona")
    @Query("select u from Usuario u where u.login = :login")
    Optional<Usuario> findByLoginForUpdate(@Param("login") String login);
}
