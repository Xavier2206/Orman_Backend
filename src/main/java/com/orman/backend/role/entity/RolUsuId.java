package com.orman.backend.role.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor
public class RolUsuId implements Serializable {

    @Column(name = "login", nullable = false, length = 30)
    private String login;

    @Column(name = "codr", nullable = false)
    private Integer codr;

    public RolUsuId(String login, Integer codr) {
        this.login = login;
        this.codr = codr;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RolUsuId rolUsuId)) {
            return false;
        }
        return Objects.equals(login, rolUsuId.login) && Objects.equals(codr, rolUsuId.codr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login, codr);
    }
}
