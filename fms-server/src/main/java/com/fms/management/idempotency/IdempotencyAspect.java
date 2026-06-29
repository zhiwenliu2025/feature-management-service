package com.fms.management.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fms.config.FmsSecurityProperties;
import com.fms.domain.IdempotencyRecordEntity;
import com.fms.repository.IdempotencyRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Aspect
@Component
public class IdempotencyAspect {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final FmsSecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(
            IdempotencyRecordRepository idempotencyRecordRepository,
            FmsSecurityProperties securityProperties,
            ObjectMapper objectMapper) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(idempotent)")
    @Transactional
    public Object handleIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        String operationKey = buildOperationKey(request);
        Optional<IdempotencyRecordEntity> existing = idempotencyRecordRepository
                .findByIdempotencyKeyAndOperationKeyAndExpiresAtAfter(idempotencyKey, operationKey, Instant.now());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Object result = joinPoint.proceed();
        if (!(result instanceof ResponseEntity<?> responseEntity)) {
            return result;
        }

        try {
            saveRecord(idempotencyKey, operationKey, responseEntity);
        } catch (DataIntegrityViolationException ex) {
            Optional<IdempotencyRecordEntity> raced = idempotencyRecordRepository
                    .findByIdempotencyKeyAndOperationKeyAndExpiresAtAfter(
                            idempotencyKey, operationKey, Instant.now());
            if (raced.isPresent()) {
                return toResponse(raced.get());
            }
            throw ex;
        }
        return result;
    }

    private void saveRecord(String idempotencyKey, String operationKey, ResponseEntity<?> responseEntity) {
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setIdempotencyKey(idempotencyKey);
        record.setOperationKey(operationKey);
        record.setResponseStatus(responseEntity.getStatusCode().value());
        record.setResponseBody(responseEntity.getBody());
        record.setResponseHeaders(extractHeaders(responseEntity.getHeaders()));
        record.setCreatedAt(Instant.now());
        record.setExpiresAt(Instant.now().plus(securityProperties.idempotency().ttl()));
        idempotencyRecordRepository.save(record);
    }

    private ResponseEntity<Object> toResponse(IdempotencyRecordEntity record) {
        HttpHeaders headers = new HttpHeaders();
        if (record.getResponseHeaders() instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if (key != null && value != null) {
                    headers.add(String.valueOf(key), String.valueOf(value));
                }
            });
        }
        return new ResponseEntity<>(record.getResponseBody(), headers, record.getResponseStatus());
    }

    private static Map<String, String> extractHeaders(HttpHeaders headers) {
        Map<String, String> stored = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            if (!values.isEmpty()) {
                stored.put(name, values.getFirst());
            }
        });
        return stored;
    }

    private static String buildOperationKey(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return request.getMethod() + " " + request.getRequestURI();
        }
        return request.getMethod() + " " + request.getRequestURI() + "?" + query;
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
