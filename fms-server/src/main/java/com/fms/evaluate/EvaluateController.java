package com.fms.evaluate;

import com.fms.evaluate.dto.BatchEvaluateRequest;
import com.fms.evaluate.dto.BatchEvaluateResponse;
import com.fms.evaluate.dto.EvaluateRequest;
import com.fms.evaluate.dto.EvaluateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/evaluate")
@Tag(name = "Evaluation", description = "Remote feature flag evaluation")
public class EvaluateController {

    private final EvaluateService evaluateService;

    public EvaluateController(EvaluateService evaluateService) {
        this.evaluateService = evaluateService;
    }

    @PostMapping("/flags/{flagKey}")
    @Operation(summary = "Evaluate a single feature flag")
    EvaluateResponse evaluateFlag(@PathVariable String flagKey, @Valid @RequestBody EvaluateRequest request) {
        return evaluateService.evaluate(flagKey, request);
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch evaluate feature flags")
    BatchEvaluateResponse evaluateBatch(@Valid @RequestBody BatchEvaluateRequest request) {
        return evaluateService.evaluateBatch(request);
    }
}
