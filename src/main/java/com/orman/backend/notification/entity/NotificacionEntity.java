package com.orman.backend.notification.entity;

import com.orman.backend.user.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

@Entity
@Table(name = "notificaciones")
@Getter
@NoArgsConstructor
public class NotificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codnot", nullable = false, updatable = false)
    private Long codnot;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "login_destinatario", nullable = false)
    private Usuario destinatario;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private NotificacionTipo tipo;

    @Setter
    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Setter
    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "referencia_tipo", nullable = false, length = 20)
    private ReferenciaTipo referenciaTipo;

    @Setter
    @Column(name = "referencia_id", nullable = false)
    private Integer referenciaId;

    @Setter
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Setter
    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        NotificacionEntity that = (NotificacionEntity) other;
        return codnot != null && Objects.equals(codnot, that.codnot);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
