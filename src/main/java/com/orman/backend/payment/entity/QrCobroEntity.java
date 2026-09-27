package com.orman.backend.payment.entity;

import com.orman.backend.person.entity.Persona;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "qr_cobro")
@Getter
@Setter
@NoArgsConstructor
public class QrCobroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codqr", nullable = false, updatable = false)
    private Integer codqr;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codper_propietaria", nullable = false)
    private Persona propietaria;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = false, length = 20)
    private String tipoContenido;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QrCobroEstado estado;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof QrCobroEntity other)) return false;
        return codqr != null && Objects.equals(codqr, other.codqr);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
