package com.orman.backend.contract.entity;

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
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "cuotas")
@Getter
@NoArgsConstructor
@DynamicInsert
public class CuotaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codcuo", nullable = false, updatable = false)
    private Integer codcuo;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codcon", nullable = false)
    private ContratoEntity contrato;

    @Setter
    @Column(name = "periodo", nullable = false)
    private LocalDate periodo;

    @Setter
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Setter
    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private CuotaEstado estado;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        CuotaEntity cuota = (CuotaEntity) other;
        return codcuo != null && Objects.equals(codcuo, cuota.codcuo);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
