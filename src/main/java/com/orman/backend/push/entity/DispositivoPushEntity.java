package com.orman.backend.push.entity;

import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.push.model.PushPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

@Entity
@Table(name = "dispositivos_push")
@Getter
@NoArgsConstructor
public class DispositivoPushEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coddis", nullable = false, updatable = false)
    private Long coddis;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sid", nullable = false, unique = true)
    private SesionUsuario sesion;

    @Column(name = "installation_id", nullable = false, length = 128)
    private String installationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    private PushPlatform platform;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public DispositivoPushEntity(SesionUsuario sesion, String installationId, PushPlatform platform,
            LocalDateTime now) {
        this.sesion = Objects.requireNonNull(sesion);
        this.installationId = Objects.requireNonNull(installationId);
        this.platform = Objects.requireNonNull(platform);
        this.activo = true;
        this.fechaRegistro = Objects.requireNonNull(now);
        this.fechaActualizacion = now;
    }

    public void replaceInstallation(String installationId, PushPlatform platform, LocalDateTime now) {
        if (!this.installationId.equals(installationId)) {
            this.installationId = Objects.requireNonNull(installationId);
            this.fechaRegistro = Objects.requireNonNull(now);
        }
        this.platform = Objects.requireNonNull(platform);
        this.activo = true;
        this.fechaActualizacion = Objects.requireNonNull(now);
    }

    public void associate(SesionUsuario sesion, PushPlatform platform, LocalDateTime now) {
        this.sesion = Objects.requireNonNull(sesion);
        this.platform = Objects.requireNonNull(platform);
        this.activo = true;
        this.fechaActualizacion = Objects.requireNonNull(now);
    }

    public void deactivate(LocalDateTime now) {
        if (activo) {
            this.activo = false;
            this.fechaActualizacion = Objects.requireNonNull(now);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        DispositivoPushEntity that = (DispositivoPushEntity) other;
        return coddis != null && Objects.equals(coddis, that.coddis);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
