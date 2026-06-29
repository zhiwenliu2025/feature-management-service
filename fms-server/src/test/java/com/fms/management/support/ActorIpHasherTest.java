package com.fms.management.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActorIpHasherTest {

    @Test
    void hashReturnsNullForBlankInput() {
        assertNull(ActorIpHasher.hash(null));
        assertNull(ActorIpHasher.hash("  "));
    }

    @Test
    void hashIsDeterministic() {
        String first = ActorIpHasher.hash("203.0.113.10");
        String second = ActorIpHasher.hash("203.0.113.10");
        assertEquals(first, second);
        assertNotEquals(ActorIpHasher.hash("203.0.113.11"), first);
    }
}
