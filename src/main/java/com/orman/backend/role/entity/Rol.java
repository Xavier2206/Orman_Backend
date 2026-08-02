package com.orman.backend.role.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

import java.util.Objects;

@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
@DynamicInsert
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codr", nullable = false, updatable = false)
    private Integer codr;

    @Setter
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Setter
    @Column(name = "estado", nullable = false)
    private Short estado;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        Rol rol = (Rol) other;
        return codr != null && Objects.equals(codr, rol.codr);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
