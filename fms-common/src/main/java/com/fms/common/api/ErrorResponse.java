package com.fms.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse of(String code, String message, String requestId, List<ErrorDetail> details) {
        return new ErrorResponse(new ErrorBody(code, message, requestId, details));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(
            String code,
            String message,
            String requestId,
            List<ErrorDetail> details
    ) {
    }

    public record ErrorDetail(String field, String issue) {
    }
}
