package com.fms.management.service;

import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.FlagEnvironmentStateEntity;
import com.fms.domain.FlagRuleEntity;
import com.fms.domain.enums.AuditAction;
import com.fms.management.dto.FlagDetailResponse;
import com.fms.management.dto.ReplaceRulesRequest;
import com.fms.management.dto.RuleInput;
import com.fms.management.dto.UpdateRuleRequest;
import com.fms.management.support.AuditRecorder;
import com.fms.repository.EnvironmentRepository;
import com.fms.repository.FlagEnvironmentStateRepository;
import com.fms.repository.FlagRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RuleService {

    private static final int MAX_RULES = 50;

    private final FlagService flagService;
    private final FlagRuleRepository flagRuleRepository;
    private final FlagEnvironmentStateRepository flagEnvironmentStateRepository;
    private final EnvironmentRepository environmentRepository;
    private final AuditRecorder auditRecorder;

    public RuleService(
            FlagService flagService,
            FlagRuleRepository flagRuleRepository,
            FlagEnvironmentStateRepository flagEnvironmentStateRepository,
            EnvironmentRepository environmentRepository,
            AuditRecorder auditRecorder) {
        this.flagService = flagService;
        this.flagRuleRepository = flagRuleRepository;
        this.flagEnvironmentStateRepository = flagEnvironmentStateRepository;
        this.environmentRepository = environmentRepository;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public FlagDetailResponse replaceRules(
            String appId, String flagKey, ReplaceRulesRequest request, String actor, String requestId) {
        validateEnvironment(request.environment());
        validateRules(request.rules());

        FeatureFlagEntity flag = flagService.findFlag(appId, flagKey);
        flagRuleRepository.deleteByFlag_IdAndEnvironment(flag.getId(), request.environment());

        for (RuleInput input : request.rules()) {
            FlagRuleEntity rule = new FlagRuleEntity();
            rule.setFlag(flag);
            rule.setEnvironment(request.environment());
            rule.setPriority(input.priority());
            rule.setName(input.name());
            rule.setConditions(input.conditions());
            rule.setValue(input.value());
            rule.setEnabled(input.isEnabled());
            flagRuleRepository.save(rule);
        }

        markEnvironmentDirty(flag, request.environment());
        auditRecorder.record(actor, AuditAction.update, "feature_flag", flagKey, request.environment(), requestId,
                Map.of("changedFields", List.of("rules")));

        return flagService.getFlag(appId, flagKey);
    }

    @Transactional
    public FlagDetailResponse updateRule(
            String appId,
            String flagKey,
            UUID ruleId,
            String environment,
            UpdateRuleRequest request,
            String actor,
            String requestId) {
        validateEnvironment(environment);
        FeatureFlagEntity flag = flagService.findFlag(appId, flagKey);
        FlagRuleEntity rule = flagRuleRepository.findById(ruleId)
                .orElseThrow(() -> new FmsException(FmsErrorCode.RULE_NOT_FOUND, "Rule not found."));
        if (!rule.getFlag().getId().equals(flag.getId()) || !rule.getEnvironment().equals(environment)) {
            throw new FmsException(FmsErrorCode.RULE_NOT_FOUND, "Rule not found.");
        }

        if (request.priority() != null) {
            rule.setPriority(request.priority());
        }
        if (request.name() != null) {
            rule.setName(request.name());
        }
        if (request.conditions() != null) {
            rule.setConditions(request.conditions());
        }
        if (request.value() != null) {
            rule.setValue(request.value());
        }
        if (request.isEnabled() != null) {
            rule.setEnabled(request.isEnabled());
        }
        flagRuleRepository.save(rule);
        markEnvironmentDirty(flag, environment);
        auditRecorder.record(actor, AuditAction.update, "feature_flag", flagKey, environment, requestId,
                Map.of("ruleId", ruleId.toString()));
        return flagService.getFlag(appId, flagKey);
    }

    private void validateRules(List<RuleInput> rules) {
        if (rules.size() > MAX_RULES) {
            throw new FmsException(FmsErrorCode.INVALID_RULES, "Maximum 50 rules per environment.");
        }
        Set<Integer> priorities = new HashSet<>();
        for (RuleInput rule : rules) {
            if (!priorities.add(rule.priority())) {
                throw new FmsException(FmsErrorCode.INVALID_RULES, "Duplicate rule priority.");
            }
            validateRolloutPercent(rule.conditions());
        }
    }

    @SuppressWarnings("unchecked")
    private void validateRolloutPercent(Object conditions) {
        if (conditions instanceof Map<?, ?> map && map.containsKey("rolloutPercent")) {
            Object value = map.get("rolloutPercent");
            if (value instanceof Number number) {
                int percent = number.intValue();
                if (percent < 0 || percent > 100) {
                    throw new FmsException(FmsErrorCode.INVALID_RULES, "rolloutPercent must be between 0 and 100.");
                }
            }
        }
    }

    private void validateEnvironment(String environment) {
        if (!environmentRepository.existsById(environment)) {
            throw new FmsException(FmsErrorCode.INVALID_ENVIRONMENT, "Environment not found: " + environment);
        }
    }

    private void markEnvironmentDirty(FeatureFlagEntity flag, String environment) {
        FlagEnvironmentStateEntity state = flagEnvironmentStateRepository
                .findByFlag_IdAndId_Environment(flag.getId(), environment)
                .orElseGet(() -> {
                    FlagEnvironmentStateEntity created = new FlagEnvironmentStateEntity();
                    created.setFlag(flag);
                    created.setEnvironment(environment);
                    return created;
                });
        state.setDraftDirty(true);
        flagEnvironmentStateRepository.save(state);
    }
}
