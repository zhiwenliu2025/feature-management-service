package com.fms.evaluate.dto;

import java.util.List;

public record BatchEvaluateResponse(
        long configVersion,
        String evaluationMode,
        List<EvaluateResponse> results,
        long latencyMs
) {
}
