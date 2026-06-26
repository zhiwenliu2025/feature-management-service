package com.fms.cache;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String appVersionPointer(String environment, String appId) {
        return "{fms:%s:%s}:snap:current".formatted(environment, appId);
    }

    public static String appSnapshot(String environment, String appId, long version) {
        return "{fms:%s:%s}:snap:v%d".formatted(environment, appId, version);
    }

    public static String appDelta(String environment, String appId, long fromVersion, long toVersion) {
        return "{fms:%s:%s}:delta:%d:%d".formatted(environment, appId, fromVersion, toVersion);
    }

    public static String environmentVersion(String environment) {
        return "{fms:%s}:env:version".formatted(environment);
    }

    public static String pubSubChannel(String environment) {
        return "fms:pubsub:%s".formatted(environment);
    }
}
