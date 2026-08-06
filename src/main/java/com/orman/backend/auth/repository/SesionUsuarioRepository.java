package com.orman.backend.auth.repository;

import com.orman.backend.auth.entity.SesionUsuario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, UUID> {

    Optional<SesionUsuario> findByUsuarioLoginAndDeviceIdAndFechaRevocacionIsNull(String login, String deviceId);

    List<SesionUsuario> findAllByUsuarioLoginAndFechaRevocacionIsNull(String login);

    List<SesionUsuario> findAllByUsuarioLoginAndFechaRevocacionIsNullOrderByFechaCreacionDesc(String login);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SesionUsuario s set s.fechaRevocacion = :now, s.motivoRevocacion = :reason "
            + "where s.usuario.login = :login and s.fechaRevocacion is null")
    int revokeAllActive(@Param("login") String login, @Param("now") java.time.LocalDateTime now,
            @Param("reason") com.orman.backend.auth.model.RevocationReason reason);
}
