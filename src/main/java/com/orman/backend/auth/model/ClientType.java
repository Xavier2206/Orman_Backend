package com.orman.backend.auth.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum ClientType {
    WEB,
    MOBILE;

    @JsonCreator
    public static ClientType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return ClientType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
