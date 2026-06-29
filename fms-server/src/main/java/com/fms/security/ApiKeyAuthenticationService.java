package com.fms.security;

import com.fms.domain.ApiKeyEntity;
import com.fms.repository.ApiKeyRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ApiKeyAuthenticationService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyAuthenticationService(ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Optional<ApiKeyPrincipal> authenticate(String plaintextKey) {
        if (plaintextKey == null || plaintextKey.isBlank()) {
            return Optional.empty();
        }

        int separator = plaintextKey.indexOf('.');
        if (separator <= 0 || separator >= plaintextKey.length() - 1) {
            return Optional.empty();
        }

        String prefix = plaintextKey.substring(0, separator);
        List<ApiKeyEntity> candidates = apiKeyRepository.findActiveByKeyPrefix(prefix);
        for (ApiKeyEntity candidate : candidates) {
            if (!passwordEncoder.matches(plaintextKey, candidate.getKeyHash())) {
                continue;
            }
            if (candidate.getExpiresAt() != null && !candidate.getExpiresAt().isAfter(Instant.now())) {
                return Optional.empty();
            }
            candidate.setLastUsedAt(Instant.now());
            apiKeyRepository.save(candidate);
            return Optional.of(new ApiKeyPrincipal(
                    candidate.getId(),
                    candidate.getApplication().getSlug(),
                    Set.copyOf(candidate.getScopes())));
        }
        return Optional.empty();
    }
}
