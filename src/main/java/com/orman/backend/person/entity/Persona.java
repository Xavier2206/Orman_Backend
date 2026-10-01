package com.orman.backend.person.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "personas")
@Getter
@NoArgsConstructor
@DynamicInsert
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codper", nullable = false, updatable = false)
    private Integer codper;

    @Setter
    @Column(name = "ci", nullable = false, unique = true, length = 20)
    private String ci;

    @Setter
    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Setter
    @Column(name = "ap", length = 40)
    private String ap;

    @Setter
    @Column(name = "am", length = 40)
    private String am;

    @Setter
    @Column(name = "genero", nullable = false, length = 1)
    private Character genero;

    @Setter
    @Column(name = "estado", nullable = false)
    private Short estado;

    @Setter
    @Column(name = "correo", nullable = false, length = 100)
    private String correo;

    @Setter
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Setter
    @Column(name = "tipo_persona", nullable = false, length = 1)
    private Character tipoPersona;

    @Setter
    @Column(name = "foto", length = 255)
    private String foto;

    @Setter
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "creada_por_login", length = 30, updatable = false)
    private String creadaPorLogin;

    public void assignCreator(String login) {
        if (creadaPorLogin != null) {
            throw new IllegalStateException("El creador de Persona es inmutable.");
        }
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("El login creador es obligatorio.");
        }
        creadaPorLogin = login;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Persona persona)) {
            return false;
        }
        return codper != null && Objects.equals(codper, persona.codper);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
