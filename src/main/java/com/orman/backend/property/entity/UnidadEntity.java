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
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "unidades")
@Getter
@NoArgsConstructor
@DynamicInsert
public class UnidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coduni", nullable = false, updatable = false)
    private Integer coduni;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codprop", nullable = false)
    private PropiedadEntity propiedad;

    @Setter
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Setter
    @Column(name = "tipo_unidad", nullable = false, length = 50)
    private String tipoUnidad;

    @Setter
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Setter
    @Column(name = "area", nullable = false, precision = 10, scale = 2)
    private BigDecimal area;

    @Setter
    @Column(name = "dormitorios", nullable = false)
    private Short dormitorios;

    @Setter
    @Column(name = "banos", nullable = false)
    private Short banos;

    @Setter
    @Column(name = "piso", nullable = false)
    private Integer piso;

    @Setter
    @Column(name = "ubicacion_interna", length = 150)
    private String ubicacionInterna;

    @Setter
    @Column(name = "precio_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioBase;

    @Setter
    @Column(name = "estado_operativo", nullable = false)
    private Short estadoOperativo;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        UnidadEntity unidad = (UnidadEntity) other;
        return coduni != null && Objects.equals(coduni, unidad.coduni);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
