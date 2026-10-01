package com.orman.backend.push.repository;

import com.orman.backend.push.entity.DispositivoPushEntity;
import com.orman.backend.push.repository.projection.PushDestinationProjection;
import com.orman.backend.auth.model.ClientType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Las consultas futuras de envío también deben exigir sesión MOBILE activa, no revocada ni expirada,
 * y Usuario y Persona activos. El indicador activo por sí solo no autoriza la entrega.
 */
public interface DispositivoPushRepository extends JpaRepository<DispositivoPushEntity, Long> {

    @Query("""
            select d.coddis as coddis, d.installationId as installationId
            from DispositivoPushEntity d
            join d.sesion s
            join s.usuario u
            join u.persona p
            where d.activo = true
              and u.login = :login
              and s.clientType = :clientType
              and s.fechaRevocacion is null
              and s.fechaExpiracion > :now
              and u.estado = :activeState
              and p.estado = :activeState
            order by d.coddis asc
            """)
    List<PushDestinationProjection> findEligibleDestinations(@Param("login") String login,
            @Param("clientType") ClientType clientType, @Param("now") LocalDateTime now,
            @Param("activeState") Short activeState);

    Optional<DispositivoPushEntity> findBySesionSid(UUID sid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DispositivoPushEntity d where d.sesion.sid = :sid")
    Optional<DispositivoPushEntity> findBySidForUpdate(@Param("sid") UUID sid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DispositivoPushEntity> findByInstallationId(String installationId);

    @Modifying
    @Query("delete from DispositivoPushEntity d where d.coddis = :coddis")
    int deleteByCoddis(@Param("coddis") Long coddis);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DispositivoPushEntity d set d.activo = false, d.fechaActualizacion = :now "
            + "where d.sesion.sid in :sids and d.activo = true")
    int deactivateBySids(@Param("sids") List<UUID> sids, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DispositivoPushEntity d set d.activo = false, d.fechaActualizacion = :now "
            + "where d.coddis = :coddis and d.activo = true")
    int deactivateByCoddis(@Param("coddis") Long coddis, @Param("now") LocalDateTime now);

}
