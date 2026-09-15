package com.orman.backend.contract.entity;

import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.entity.UnidadEntity;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "contratos")
@Getter
@NoArgsConstructor
@DynamicInsert
public class ContratoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codcon", nullable = false, updatable = false)
    private Integer codcon;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coduni", nullable = false)
    private UnidadEntity unidad;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codper_inquilino", nullable = false)
    private Persona inquilino;

    @Setter
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Setter
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Setter
    @Column(name = "monto_mensual", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoMensual;

    @Setter
    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Setter
    @Column(name = "garantia", nullable = false, precision = 14, scale = 2)
    private BigDecimal garantia;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private ContratoEstado estado;

    @Setter
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Setter
    @Column(name = "fecha_rescision")
    private LocalDate fechaRescision;

    @Setter
    @Column(name = "motivo_rescision", length = 500)
    private String motivoRescision;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        ContratoEntity contrato = (ContratoEntity) other;
        return codcon != null && Objects.equals(codcon, contrato.codcon);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
