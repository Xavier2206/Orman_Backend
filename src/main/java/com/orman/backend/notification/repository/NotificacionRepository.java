package com.orman.backend.notification.repository;

import com.orman.backend.notification.entity.NotificacionEntity;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.ReferenciaTipo;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {

    @EntityGraph(attributePaths = "destinatario")
    @Query("select n from NotificacionEntity n where n.codnot = :codnot")
    Optional<NotificacionEntity> findForPush(@Param("codnot") Long codnot);

    @Query(value = """
            select n from NotificacionEntity n
            where n.destinatario.login = :login
              and (:tipo is null or n.tipo = :tipo)
              and (:leida is null
                   or (:leida = true and n.fechaLectura is not null)
                   or (:leida = false and n.fechaLectura is null))
            """, countQuery = """
            select count(n) from NotificacionEntity n
            where n.destinatario.login = :login
              and (:tipo is null or n.tipo = :tipo)
              and (:leida is null
                   or (:leida = true and n.fechaLectura is not null)
                   or (:leida = false and n.fechaLectura is null))
            """)
    Page<NotificacionEntity> searchOwn(@Param("login") String login, @Param("tipo") NotificacionTipo tipo,
                                       @Param("leida") Boolean leida, Pageable pageable);

    Optional<NotificacionEntity> findByCodnotAndDestinatarioLogin(Long codnot, String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n from NotificacionEntity n
            where n.codnot = :codnot and n.destinatario.login = :login
            """)
    Optional<NotificacionEntity> findOwnForUpdate(@Param("codnot") Long codnot, @Param("login") String login);

    long countByDestinatarioLoginAndFechaLecturaIsNull(String login);

    Optional<NotificacionEntity> findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
            String login, NotificacionTipo tipo, ReferenciaTipo referenciaTipo, Integer referenciaId);

    List<NotificacionEntity> findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
            String login, ReferenciaTipo referenciaTipo, Integer referenciaId);
}
