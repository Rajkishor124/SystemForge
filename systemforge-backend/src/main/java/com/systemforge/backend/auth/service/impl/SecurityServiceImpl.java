package com.systemforge.backend.auth.service.impl;

import com.systemforge.backend.auth.service.SecurityService;
import com.systemforge.backend.auth.util.SecurityPrincipalUtil;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation of SecurityService that delegates to SecurityPrincipalUtil.
 */
@Service
public class SecurityServiceImpl implements SecurityService {

    @Override
    public UUID getAuthenticatedUserId() {
        return SecurityPrincipalUtil.getAuthenticatedUserId();
    }

    @Override
    public String getAuthenticatedRole() {
        return SecurityPrincipalUtil.getAuthenticatedRole();
    }

    @Override
    public boolean isAdmin() {
        return SecurityPrincipalUtil.isAdmin();
    }

    @Override
    public boolean isDeveloper() {
        return SecurityPrincipalUtil.isDeveloper();
    }

    @Override
    public boolean isAuthenticated() {
        return SecurityPrincipalUtil.isAuthenticated();
    }
}
