package com.orman.backend.auth.repository;

import com.orman.backend.auth.entity.SesionUsuario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, UUID> {

    Optional<SesionUsuario> findByUsuarioLoginAndDeviceIdAndFechaRevocacionIsNull(String login, String deviceId);

    List<SesionUsuario> findAllByUsuarioLoginAndFechaRevocacionIsNull(String login);

    List<SesionUsuario> findAllByUsuarioLoginAndFechaRevocacionIsNullOrderByFechaCreacionDesc(String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SesionUsuario s where s.usuario.login = :login and s.fechaRevocacion is null")
    List<SesionUsuario> findAllActiveForUpdate(@Param("login") String login);

    boolean existsByUsuarioLoginAndDeviceIdAndFechaRevocacionIsNull(String login, String deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"usuario", "usuario.persona"})
    @Query("select s from SesionUsuario s where s.sid = :sid")
    Optional<SesionUsuario> findBySidForUpdate(@Param("sid") UUID sid);

    @EntityGraph(attributePaths = {"usuario", "usuario.persona"})
    @Query("select s from SesionUsuario s where s.sid = :sid")
    Optional<SesionUsuario> findForAuthentication(@Param("sid") UUID sid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SesionUsuario s where s.sid = :sid and s.usuario.login = :login")
    Optional<SesionUsuario> findOwnedForUpdate(@Param("sid") UUID sid, @Param("login") String login);
}
