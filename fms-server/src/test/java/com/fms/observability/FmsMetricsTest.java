package com.fms.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FmsMetricsTest {

    @Test
    void safeTagTruncatesLongValues() {
        assertEquals("unknown", FmsMetrics.safeTag(null));
        assertEquals("unknown", FmsMetrics.safeTag("  "));
        assertEquals("short", FmsMetrics.safeTag("short"));
        assertEquals(64, FmsMetrics.safeTag("x".repeat(100)).length());
    }
}
