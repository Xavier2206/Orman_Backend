package com.orman.backend.auth.model;

public enum RevocationReason {
    REPLACED_BY_NEW_LOGIN,
    REFRESH_REUSE,
    EXPIRED,
    LOGOUT,
    LOGOUT_ALL,
    ADMIN_REVOKED,
    PASSWORD_CHANGED,
    USER_DISABLED,
    PERSON_DISABLED
}
