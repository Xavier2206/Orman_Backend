package com.orman.backend.menu.entity;

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
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor
@DynamicInsert
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codm", nullable = false, updatable = false)
    private Integer codm;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "icono", length = 50)
    private String icono;

    @Column(name = "estado", nullable = false)
    private Short estado;

    public Menu(String nombre, String icono, Short estado) {
        this.nombre = nombre;
        this.icono = icono;
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
        Menu menu = (Menu) other;
        return codm != null && Objects.equals(codm, menu.codm);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
