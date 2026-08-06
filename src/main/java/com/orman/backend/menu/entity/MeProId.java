package com.orman.backend.menu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class MeProId implements Serializable {

    @Column(name = "codm", nullable = false)
    private Integer codm;

    @Column(name = "codp", nullable = false)
    private Integer codp;

    public MeProId(Integer codm, Integer codp) {
        this.codm = codm;
        this.codp = codp;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MeProId meProId)) {
            return false;
        }
        return Objects.equals(codm, meProId.codm) && Objects.equals(codp, meProId.codp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codm, codp);
    }
}
