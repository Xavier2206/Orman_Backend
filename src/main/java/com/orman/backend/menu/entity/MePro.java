package com.orman.backend.menu.entity;

import com.orman.backend.process.entity.Proceso;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

@Entity
@Table(name = "mepro")
@Getter
@NoArgsConstructor
public class MePro {

    @EmbeddedId
    private MeProId id;

    @MapsId("codm")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codm", nullable = false)
    private Menu menu;

    @MapsId("codp")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codp", nullable = false)
    private Proceso proceso;

    public MePro(Menu menu, Proceso proceso) {
        this.id = new MeProId(menu.getCodm(), proceso.getCodp());
        this.menu = menu;
        this.proceso = proceso;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        MePro mePro = (MePro) other;
        return id != null && Objects.equals(id, mePro.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
