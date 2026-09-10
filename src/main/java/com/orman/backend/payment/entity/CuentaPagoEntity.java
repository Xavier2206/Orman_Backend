package com.orman.backend.payment.entity;

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
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cuentas_pago")
@Getter
@Setter
@NoArgsConstructor
public class CuentaPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codcta")
    private Integer codcta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codper_propietaria", nullable = false)
    private Persona propietaria;

    @Column(nullable = false, length = 100)
    private String banco;

    @Column(name = "numero_cuenta", nullable = false, length = 100)
    private String numeroCuenta;

    @Column(nullable = false, length = 200)
    private String titular;

    @Column(name = "qr_url", length = 500)
    private String qrUrl;

    @Column(length = 1000)
    private String instrucciones;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Short estado;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof CuentaPagoEntity other)) return false;
        return codcta != null && Objects.equals(codcta, other.codcta);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
