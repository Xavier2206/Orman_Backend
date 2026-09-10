package com.orman.backend.property.entity;

import com.orman.backend.person.entity.Persona;
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
@Table(name = "propiedades")
@Getter
@NoArgsConstructor
@DynamicInsert
public class PropiedadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codprop", nullable = false, updatable = false)
    private Integer codprop;

    @Setter
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Setter
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Setter
    @Column(name = "direccion", nullable = false, length = 200)
    private String direccion;

    @Setter
    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    @Setter
    @Column(name = "referencia", length = 255)
    private String referencia;

    @Setter
    @Column(name = "latitud", precision = 9, scale = 6)
    private BigDecimal latitud;

    @Setter
    @Column(name = "longitud", precision = 9, scale = 6)
    private BigDecimal longitud;

    @Setter
    @Column(name = "portada_url", length = 500)
    private String portadaUrl;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codper_propietaria", nullable = false)
    private Persona propietaria;

    @Setter
    @Column(name = "inversion_inicial", nullable = false, precision = 14, scale = 2)
    private BigDecimal inversionInicial;

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
        PropiedadEntity propiedad = (PropiedadEntity) other;
        return codprop != null && Objects.equals(codprop, propiedad.codprop);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
