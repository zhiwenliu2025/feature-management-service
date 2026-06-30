package com.fms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "fms.security")
public class FmsSecurityProperties {

    private final ApiKey apiKey = new ApiKey();
    private final Idempotency idempotency = new Idempotency();
    private boolean localApiAutoAuth = false;

    public ApiKey apiKey() {
        return apiKey;
    }

    public Idempotency idempotency() {
        return idempotency;
    }

    public boolean localApiAutoAuth() {
        return localApiAutoAuth;
    }

    public void setLocalApiAutoAuth(boolean localApiAutoAuth) {
        this.localApiAutoAuth = localApiAutoAuth;
    }

    public static class ApiKey {
        private boolean enforced = true;

        public boolean enforced() {
            return enforced;
        }

        public void setEnforced(boolean enforced) {
            this.enforced = enforced;
        }
    }

    public static class Idempotency {
        private Duration ttl = Duration.ofHours(24);

        public Duration ttl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}
