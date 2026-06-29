package com.fms.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestContextFilterTest {

    @Test
    void resolveModuleFromApiPath() {
        assertEquals("management", RequestContextFilter.resolveModule("/api/v1/management/flags"));
        assertEquals("sync", RequestContextFilter.resolveModule("/api/v1/sync/snapshot"));
        assertEquals("evaluate", RequestContextFilter.resolveModule("/api/v1/evaluate/flags/checkout_v2"));
        assertEquals("explain", RequestContextFilter.resolveModule("/api/v1/explain/flags/checkout_v2"));
        assertEquals("platform", RequestContextFilter.resolveModule("/api/health"));
    }
}
