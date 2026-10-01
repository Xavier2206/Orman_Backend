package com.orman.backend.common.error;

/**
 * Stable error identifiers exposed by the HTTP error contract.
 */
public enum ErrorCode {

    INVALID_CREDENTIALS,
    INVALID_REFRESH_TOKEN,
    INVALID_TOKEN,
    TOKEN_EXPIRED,
    SESSION_REVOKED,
    SESSION_EXPIRED,
    ACCESS_DENIED,
    LAST_OWNER_REQUIRED,
    RESOURCE_NOT_FOUND,
    VALIDATION_ERROR,
    INVALID_REQUEST,
    BUSINESS_RULE_VIOLATION,
    CONFLICT,
    INTERNAL_ERROR
}
