package com.fms.security;

import com.fms.domain.ApiKeyEntity;
import com.fms.domain.ApplicationEntity;
import com.fms.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationServiceTest {

    private static final String PLAINTEXT_KEY = "fms_ab12.cdef1234567890abcdef1234567890";

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private PasswordEncoder passwordEncoder;
    private ApiKeyAuthenticationService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new ApiKeyAuthenticationService(apiKeyRepository, passwordEncoder);
    }

    @Test
    void authenticateReturnsPrincipalForValidKey() {
        ApiKeyEntity entity = activeKeyEntity();
        when(apiKeyRepository.findActiveByKeyPrefix("fms_ab12")).thenReturn(List.of(entity));

        Optional<ApiKeyPrincipal> principal = service.authenticate(PLAINTEXT_KEY);

        assertTrue(principal.isPresent());
        assertEquals("checkout-service", principal.get().appId());
        assertTrue(principal.get().hasScope("sync"));
        assertTrue(principal.get().hasScope("evaluate"));
        verify(apiKeyRepository).save(entity);
    }

    @Test
    void authenticateRejectsExpiredKey() {
        ApiKeyEntity entity = activeKeyEntity();
        entity.setExpiresAt(Instant.now().minusSeconds(60));
        when(apiKeyRepository.findActiveByKeyPrefix("fms_ab12")).thenReturn(List.of(entity));

        Optional<ApiKeyPrincipal> principal = service.authenticate(PLAINTEXT_KEY);

        assertTrue(principal.isEmpty());
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void authenticateRejectsInvalidKeyFormat() {
        Optional<ApiKeyPrincipal> principal = service.authenticate("invalid-key");

        assertTrue(principal.isEmpty());
        verify(apiKeyRepository, never()).findActiveByKeyPrefix(any());
    }

    private ApiKeyEntity activeKeyEntity() {
        ApplicationEntity application = new ApplicationEntity();
        application.setSlug("checkout-service");

        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setApplication(application);
        entity.setKeyPrefix("fms_ab12");
        entity.setKeyHash(passwordEncoder.encode(PLAINTEXT_KEY));
        entity.setScopes(List.of("sync", "evaluate", "explain:read"));
        return entity;
    }
}
