package com.orman.backend.common.error;

/**
 * Stable error identifiers exposed by the HTTP error contract.
 */
public enum ErrorCode {

    RESOURCE_NOT_FOUND,
    VALIDATION_ERROR,
    INVALID_REQUEST,
    BUSINESS_RULE_VIOLATION,
    CONFLICT,
    INTERNAL_ERROR
}
