package com.fms.security;

import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DataPlaneAuthzService {

    public void requireScope(String scope) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ApiKeyPrincipal principal)) {
            return;
        }
        if (!principal.hasScope(scope)) {
            throw new FmsException(FmsErrorCode.FORBIDDEN, "Insufficient scope for this operation.");
        }
    }
}
