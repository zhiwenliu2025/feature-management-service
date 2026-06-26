package com.fms.management.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Profile("!local")
public class ManagementScopeAspect {

    private final ManagementAuthzService authzService;

    public ManagementScopeAspect(ManagementAuthzService authzService) {
        this.authzService = authzService;
    }

    @Before("@annotation(requiresScope)")
    public void checkScope(RequiresScope requiresScope) {
        authzService.checkScope(requiresScope.value());
    }
}
