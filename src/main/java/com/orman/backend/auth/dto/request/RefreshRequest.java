package com.orman.backend.auth.dto.request;

public record RefreshRequest(String refreshToken) {

    @Override
    public String toString() {
        return "RefreshRequest[refreshToken=<redacted>]";
    }
}
