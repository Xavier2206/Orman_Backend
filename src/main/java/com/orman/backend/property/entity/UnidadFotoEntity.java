package com.orman.backend.property.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "unidad_fotos")
@Getter
@NoArgsConstructor
@DynamicInsert
public class UnidadFotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coduni", nullable = false)
    private UnidadEntity unidad;

    @Setter
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Setter
    @Column(name = "titulo", length = 150)
    private String titulo;

    @Setter
    @Column(name = "ambiente", length = 100)
    private String ambiente;

    @Setter
    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Setter
    @Column(name = "portada", nullable = false)
    private Boolean portada;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        UnidadFotoEntity foto = (UnidadFotoEntity) other;
        return id != null && Objects.equals(id, foto.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
