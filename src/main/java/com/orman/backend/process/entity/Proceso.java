package com.orman.backend.process.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "procesos")
@Getter
@Setter
@NoArgsConstructor
@DynamicInsert
public class Proceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codp", nullable = false, updatable = false)
    private Integer codp;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "enlace", nullable = false, length = 60)
    private String enlace;

    @Column(name = "estado", nullable = false)
    private Short estado;

    public Proceso(String nombre, String enlace, Short estado) {
        this.nombre = nombre;
        this.enlace = enlace;
        this.estado = estado;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        Proceso proceso = (Proceso) other;
        return codp != null && Objects.equals(codp, proceso.codp);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
