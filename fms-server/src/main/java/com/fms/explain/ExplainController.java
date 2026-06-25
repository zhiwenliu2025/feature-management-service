package com.fms.explain;

import com.fms.explain.dto.ExplainRequest;
import com.fms.explain.dto.ExplainResponse;
import com.fms.explain.dto.ReplayExplainRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/explain")
@Tag(name = "Explain", description = "Evaluation explainability and replay")
public class ExplainController {

    private final ExplainService explainService;

    public ExplainController(ExplainService explainService) {
        this.explainService = explainService;
    }

    @PostMapping("/flags/{flagKey}")
    @Operation(summary = "Explain current evaluation")
    ExplainResponse explain(@PathVariable String flagKey, @Valid @RequestBody ExplainRequest request) {
        return explainService.explain(flagKey, request);
    }

    @PostMapping("/flags/{flagKey}/replay")
    @Operation(summary = "Replay evaluation at a point in time")
    ExplainResponse replay(@PathVariable String flagKey, @Valid @RequestBody ReplayExplainRequest request) {
        return explainService.replay(flagKey, request);
    }
}
