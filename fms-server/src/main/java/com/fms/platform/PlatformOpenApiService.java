package com.fms.platform;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PlatformOpenApiService {

    private final OpenApiWebMvcResource openApiWebMvcResource;
    private final String springDocApiDocsPath;

    public PlatformOpenApiService(
            OpenApiWebMvcResource openApiWebMvcResource,
            @Value("${springdoc.api-docs.path:/v3/api-docs}") String springDocApiDocsPath) {
        this.openApiWebMvcResource = openApiWebMvcResource;
        this.springDocApiDocsPath = springDocApiDocsPath;
    }

    byte[] openApiDocument(HttpServletRequest request, Locale locale) {
        try {
            return openApiWebMvcResource.openapiJson(request, springDocApiDocsPath, locale);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OpenAPI document", ex);
        }
    }
}
