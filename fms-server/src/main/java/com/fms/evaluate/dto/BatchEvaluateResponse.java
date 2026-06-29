package com.fms.evaluate.dto;

import java.util.List;

public record BatchEvaluateResponse(
        long configVersion,
        String evaluationMode,
        List<BatchFlagResult> results,
        long latencyMs
) {
}
