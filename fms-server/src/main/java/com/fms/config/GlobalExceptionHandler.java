package com.fms.config;

import com.fms.common.api.ErrorResponse;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FmsException.class)
    ResponseEntity<ErrorResponse> handleFmsException(FmsException ex, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return ResponseEntity
                .status(ex.errorCode().httpStatus())
                .body(ErrorResponse.of(
                        ex.errorCode().name(),
                        ex.getMessage(),
                        requestId,
                        ex.details().isEmpty() ? null : ex.details()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        FmsErrorCode.VALIDATION_ERROR.name(),
                        "Request validation failed.",
                        request.getHeader("X-Request-Id"),
                        details));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(
                        FmsErrorCode.INTERNAL_ERROR.name(),
                        "An unexpected error occurred.",
                        request.getHeader("X-Request-Id"),
                        null));
    }

    private ErrorResponse.ErrorDetail toDetail(FieldError error) {
        return new ErrorResponse.ErrorDetail(error.getField(), error.getDefaultMessage());
    }
}
