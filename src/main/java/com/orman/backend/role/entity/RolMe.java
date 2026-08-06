package com.orman.backend.role.entity;

import com.orman.backend.menu.entity.Menu;
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
@Table(name = "rolme")
@Getter
@NoArgsConstructor
public class RolMe {

    @EmbeddedId
    private RolMeId id;

    @MapsId("codr")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codr", nullable = false)
    private Rol rol;

    @MapsId("codm")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codm", nullable = false)
    private Menu menu;

    public RolMe(Rol rol, Menu menu) {
        this.id = new RolMeId(rol.getCodr(), menu.getCodm());
        this.rol = rol;
        this.menu = menu;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        RolMe rolMe = (RolMe) other;
        return id != null && Objects.equals(id, rolMe.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
