package com.orman.backend.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recibos")
@Getter
@Setter
@NoArgsConstructor
public class ReciboEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codrec")
    private Integer codrec;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codpag", nullable = false, unique = true)
    private PagoEntity pago;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ReciboEntity other)) return false;
        return codrec != null && Objects.equals(codrec, other.codrec);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
