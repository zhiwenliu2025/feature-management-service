package com.fms.common.exception;

public enum FmsErrorCode {
    VALIDATION_ERROR(400),
    INVALID_FLAG_KEY(400),
    INVALID_CONDITIONS(400),
    DELTA_VERSION_GAP_TOO_LARGE(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    FLAG_NOT_FOUND(404),
    APPLICATION_NOT_FOUND(404),
    RELEASE_NOT_FOUND(404),
    VERSION_NOT_FOUND(404),
    FLAG_ALREADY_EXISTS(409),
    PUBLISH_IN_PROGRESS(409),
    ROLLBACK_TARGET_NOT_FOUND(422),
    RATE_LIMIT_EXCEEDED(429),
    INTERNAL_ERROR(500),
    SERVICE_UNAVAILABLE(503);

    private final int httpStatus;

    FmsErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
