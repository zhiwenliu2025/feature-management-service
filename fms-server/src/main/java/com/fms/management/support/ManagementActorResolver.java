package com.fms.management.support;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class ManagementActorResolver {

    public String resolve(Jwt jwt) {
        if (jwt == null) {
            return "local-dev";
        }
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getSubject();
    }
}
