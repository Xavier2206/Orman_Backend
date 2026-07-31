package com.orman.backend.user.entity;

import com.orman.backend.person.entity.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor
@DynamicInsert
public class Usuario {

    @Id
    @Setter
    @Column(name = "login", nullable = false, updatable = false, length = 30)
    private String login;

    @Setter
    @Column(name = "passwd", nullable = false, length = 255)
    private String passwd;

    @Setter
    @Column(name = "estado", nullable = false)
    private Short estado;

    @Setter
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codper", nullable = false, unique = true)
    private Persona persona;

    @Setter
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Setter
    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        Usuario usuario = (Usuario) other;
        return login != null && Objects.equals(login, usuario.login);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
