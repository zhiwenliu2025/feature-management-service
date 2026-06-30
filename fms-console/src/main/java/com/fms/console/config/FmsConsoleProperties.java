package com.fms.console.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fms.console")
public record FmsConsoleProperties(
        String apiBaseUrl,
        String localApiKey
) {
    public FmsConsoleProperties {
        if (apiBaseUrl == null) {
            apiBaseUrl = "";
        }
        if (localApiKey == null) {
            localApiKey = "";
        }
    }
}
