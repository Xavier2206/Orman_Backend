package com.orman.backend.role.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class RolMeId implements Serializable {

    @Column(name = "codr", nullable = false)
    private Integer codr;

    @Column(name = "codm", nullable = false)
    private Integer codm;

    public RolMeId(Integer codr, Integer codm) {
        this.codr = codr;
        this.codm = codm;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RolMeId rolMeId)) {
            return false;
        }
        return Objects.equals(codr, rolMeId.codr) && Objects.equals(codm, rolMeId.codm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codr, codm);
    }
}
