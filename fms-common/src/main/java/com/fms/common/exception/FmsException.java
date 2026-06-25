package com.fms.common.exception;

import com.fms.common.api.ErrorResponse;

import java.util.List;

public class FmsException extends RuntimeException {

    private final FmsErrorCode errorCode;
    private final List<ErrorResponse.ErrorDetail> details;

    public FmsException(FmsErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public FmsException(FmsErrorCode errorCode, String message, List<ErrorResponse.ErrorDetail> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public FmsErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorResponse.ErrorDetail> details() {
        return details;
    }
}
