package com.orman.backend.role.entity;

import com.orman.backend.user.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "rolusu")
@Getter
@NoArgsConstructor
@DynamicInsert
public class RolUsu {

    @EmbeddedId
    private RolUsuId id;

    @MapsId("login")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "login", nullable = false)
    private Usuario usuario;

    @MapsId("codr")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codr", nullable = false)
    private Rol rol;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    public RolUsu(Usuario usuario, Rol rol) {
        this.id = new RolUsuId(usuario.getLogin(), rol.getCodr());
        this.usuario = usuario;
        this.rol = rol;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        RolUsu rolUsu = (RolUsu) other;
        return id != null && Objects.equals(id, rolUsu.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
