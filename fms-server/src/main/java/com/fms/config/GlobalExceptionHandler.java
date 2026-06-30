package com.fms.config;

import com.fms.common.api.ErrorResponse;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.observability.FmsMetrics;
import com.fms.observability.RequestContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final FmsMetrics metrics;

    public GlobalExceptionHandler(FmsMetrics metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(FmsException.class)
    ResponseEntity<ErrorResponse> handleFmsException(FmsException ex, HttpServletRequest request) {
        String requestId = request.getHeader(RequestContextFilter.REQUEST_ID_HEADER);
        String module = MDC.get(RequestContextFilter.MDC_MODULE);
        metrics.recordError(ex.errorCode().name(), module == null ? "unknown" : module);

        if (ex.errorCode().httpStatus() >= 500) {
            log.error("Request failed requestId={} errorCode={} path={}",
                    requestId, ex.errorCode().name(), request.getRequestURI(), ex);
        } else {
            log.warn("Request rejected requestId={} errorCode={} path={} message={}",
                    requestId, ex.errorCode().name(), request.getRequestURI(), ex.getMessage());
        }

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
        String module = MDC.get(RequestContextFilter.MDC_MODULE);
        metrics.recordError(FmsErrorCode.VALIDATION_ERROR.name(), module == null ? "unknown" : module);

        List<ErrorResponse.ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        FmsErrorCode.VALIDATION_ERROR.name(),
                        "Request validation failed.",
                        request.getHeader(RequestContextFilter.REQUEST_ID_HEADER),
                        details));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        String module = MDC.get(RequestContextFilter.MDC_MODULE);
        metrics.recordError(FmsErrorCode.VALIDATION_ERROR.name(), module == null ? "unknown" : module);
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        FmsErrorCode.VALIDATION_ERROR.name(),
                        "Required request parameter '" + ex.getParameterName() + "' is missing.",
                        request.getHeader(RequestContextFilter.REQUEST_ID_HEADER),
                        null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path != null && path.endsWith("/favicon.ico")) {
            log.debug("Static resource not found path={}", path);
        } else {
            log.debug("No handler for path={}", path);
        }
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "NOT_FOUND",
                        "Resource not found.",
                        request.getHeader(RequestContextFilter.REQUEST_ID_HEADER),
                        null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String module = MDC.get(RequestContextFilter.MDC_MODULE);
        metrics.recordError(FmsErrorCode.INTERNAL_ERROR.name(), module == null ? "unknown" : module);
        log.error("Unhandled exception requestId={} path={}",
                request.getHeader(RequestContextFilter.REQUEST_ID_HEADER),
                request.getRequestURI(),
                ex);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(
                        FmsErrorCode.INTERNAL_ERROR.name(),
                        "An unexpected error occurred.",
                        request.getHeader(RequestContextFilter.REQUEST_ID_HEADER),
                        null));
    }

    private ErrorResponse.ErrorDetail toDetail(FieldError error) {
        return new ErrorResponse.ErrorDetail(error.getField(), error.getDefaultMessage());
    }
}
