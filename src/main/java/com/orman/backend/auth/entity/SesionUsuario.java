package com.orman.backend.auth.entity;

import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.user.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

@Entity
@Table(name = "sesiones_usuario")
@Getter
@NoArgsConstructor
public class SesionUsuario {

    @Id
    @Column(name = "sid", nullable = false, updatable = false)
    private UUID sid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "login", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "refresh_token_version", nullable = false)
    private Integer refreshTokenVersion;

    @Column(name = "device_id", nullable = false, updatable = false, length = 100)
    private String deviceId;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, updatable = false, length = 10)
    private ClientType clientType;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "ultimo_uso", nullable = false)
    private LocalDateTime ultimoUso;

    @Column(name = "fecha_revocacion")
    private LocalDateTime fechaRevocacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_revocacion", length = 40)
    private RevocationReason motivoRevocacion;

    public SesionUsuario(UUID sid, Usuario usuario, String refreshTokenHash, String deviceId,
            String deviceName, ClientType clientType, LocalDateTime now, LocalDateTime expiration) {
        this.sid = Objects.requireNonNull(sid);
        this.usuario = Objects.requireNonNull(usuario);
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash);
        this.refreshTokenVersion = 1;
        this.deviceId = Objects.requireNonNull(deviceId);
        this.deviceName = Objects.requireNonNull(deviceName);
        this.clientType = Objects.requireNonNull(clientType);
        this.fechaCreacion = Objects.requireNonNull(now);
        this.fechaExpiracion = Objects.requireNonNull(expiration);
        this.ultimoUso = now;
    }

    public boolean isRevoked() {
        return fechaRevocacion != null;
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return !fechaExpiracion.isAfter(now);
    }

    public void revoke(LocalDateTime now, RevocationReason reason) {
        if (!isRevoked()) {
            this.fechaRevocacion = Objects.requireNonNull(now);
            this.motivoRevocacion = Objects.requireNonNull(reason);
        }
    }

    public void rotateRefreshToken(String hash, LocalDateTime now) {
        this.refreshTokenHash = Objects.requireNonNull(hash);
        this.refreshTokenVersion++;
        this.ultimoUso = Objects.requireNonNull(now);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        SesionUsuario sesion = (SesionUsuario) other;
        return sid != null && Objects.equals(sid, sesion.sid);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
