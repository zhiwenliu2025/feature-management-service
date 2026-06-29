package com.fms.ruleengine;

import java.nio.charset.StandardCharsets;

/**
 * MurmurHash3 32-bit implementation for stable rollout bucketing.
 */
public final class MurmurHash3 {

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    private MurmurHash3() {
    }

    public static int hash32(String input) {
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        int hash = 0;
        int length = data.length;
        int roundedEnd = length & ~3;

        for (int i = 0; i < roundedEnd; i += 4) {
            int k = (data[i] & 0xff)
                    | ((data[i + 1] & 0xff) << 8)
                    | ((data[i + 2] & 0xff) << 16)
                    | ((data[i + 3] & 0xff) << 24);
            k *= C1;
            k = Integer.rotateLeft(k, 15);
            k *= C2;
            hash ^= k;
            hash = Integer.rotateLeft(hash, 13);
            hash = hash * 5 + 0xe6546b64;
        }

        int k1 = 0;
        switch (length & 3) {
            case 3 -> k1 ^= (data[roundedEnd + 2] & 0xff) << 16;
            case 2 -> k1 ^= (data[roundedEnd + 1] & 0xff) << 8;
            case 1 -> {
                k1 ^= data[roundedEnd] & 0xff;
                k1 *= C1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= C2;
                hash ^= k1;
            }
            default -> {
            }
        }

        hash ^= length;
        return fmix32(hash);
    }

    public static int bucket(String flagKey, String bucketingKey, String rolloutSalt) {
        String input = flagKey + bucketingKey + (rolloutSalt == null ? "" : rolloutSalt);
        return Math.floorMod(hash32(input), 10_000);
    }

    private static int fmix32(int hash) {
        hash ^= hash >>> 16;
        hash *= 0x85ebca6b;
        hash ^= hash >>> 13;
        hash *= 0xc2b2ae35;
        hash ^= hash >>> 16;
        return hash;
    }
}
