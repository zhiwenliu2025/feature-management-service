package com.fms.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Propagates correlation IDs and safe request context into the logging MDC.
 * # SECURITY-REVIEW: Never place userId or other PII in MDC.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_MODULE = "module";
    static final String MDC_APP_ID = "appId";
    static final String MDC_ENVIRONMENT = "environment";
    static final String MDC_TRACE_ID = "traceId";
    static final String MDC_SPAN_ID = "spanId";

    private final Optional<Tracer> tracer;

    public RequestContextFilter(Optional<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_MODULE, resolveModule(request.getRequestURI()));
            String appId = request.getParameter("appId");
            if (appId != null && !appId.isBlank()) {
                MDC.put(MDC_APP_ID, appId);
            }
            String environment = request.getParameter("environment");
            if (environment == null || environment.isBlank()) {
                environment = extractEnvironmentFromPath(request.getRequestURI());
            }
            if (environment != null && !environment.isBlank()) {
                MDC.put(MDC_ENVIRONMENT, environment);
            }
            enrichTraceContext();
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_MODULE);
            MDC.remove(MDC_APP_ID);
            MDC.remove(MDC_ENVIRONMENT);
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_SPAN_ID);
        }
    }

    private void enrichTraceContext() {
        tracer.ifPresent(t -> {
            Span span = t.currentSpan();
            if (span == null) {
                return;
            }
            String traceId = span.context().traceId();
            String spanId = span.context().spanId();
            if (traceId != null) {
                MDC.put(MDC_TRACE_ID, traceId);
            }
            if (spanId != null) {
                MDC.put(MDC_SPAN_ID, spanId);
            }
        });
    }

    private static String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return "req_" + UUID.randomUUID();
        }
        return requestId.length() > 128 ? requestId.substring(0, 128) : requestId;
    }

    static String resolveModule(String path) {
        if (path == null) {
            return "unknown";
        }
        if (path.startsWith("/api/v1/management")) {
            return "management";
        }
        if (path.startsWith("/api/v1/sync")) {
            return "sync";
        }
        if (path.startsWith("/api/v1/evaluate")) {
            return "evaluate";
        }
        if (path.startsWith("/api/v1/explain")) {
            return "explain";
        }
        if (path.startsWith("/api/health") || path.startsWith("/api/ready")) {
            return "platform";
        }
        return "other";
    }

    private static String extractEnvironmentFromPath(String path) {
        if (path == null) {
            return null;
        }
        int envIndex = path.indexOf("/environments/");
        if (envIndex < 0) {
            return null;
        }
        String remainder = path.substring(envIndex + "/environments/".length());
        int slash = remainder.indexOf('/');
        return slash < 0 ? remainder : remainder.substring(0, slash);
    }
}
