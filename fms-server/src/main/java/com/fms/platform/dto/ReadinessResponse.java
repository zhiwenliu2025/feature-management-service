package com.fms.platform.dto;

import java.util.Map;

public record ReadinessResponse(String status, Map<String, String> checks) {
}
