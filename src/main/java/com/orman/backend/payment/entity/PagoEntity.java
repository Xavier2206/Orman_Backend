package com.orman.backend.payment.entity;

import com.orman.backend.contract.entity.CuotaEntity;
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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
public class PagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codpag")
    private Integer codpag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codcuo", nullable = false)
    private CuotaEntity cuota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codcta")
    private CuentaPagoEntity cuentaPago;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Column(name = "referencia_externa", length = 100)
    private String referenciaExterna;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PagoEstado estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenPago origen;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PagoEntity other)) return false;
        return codpag != null && Objects.equals(codpag, other.codpag);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
