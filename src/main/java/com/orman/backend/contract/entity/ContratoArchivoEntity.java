package com.orman.backend.contract.entity;

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
@Table(name = "contrato_archivos")
@Getter
@NoArgsConstructor
@DynamicInsert
public class ContratoArchivoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codcon", nullable = false)
    private ContratoEntity contrato;

    @Setter
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Setter
    @Column(name = "nombre_archivo", nullable = false, length = 200)
    private String nombreArchivo;

    @Setter
    @Column(name = "tipo_contenido", length = 100)
    private String tipoContenido;

    @Setter
    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        ContratoArchivoEntity archivo = (ContratoArchivoEntity) other;
        return id != null && Objects.equals(id, archivo.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
