package com.systemforge.backend.playground.feature.impl;

import com.systemforge.backend.playground.engine.TemplateCompositionContext;
import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;
import com.systemforge.backend.playground.feature.FeatureModule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Feature Module: Role-Based Access Control (RBAC).
 *
 * <p>Injects role hierarchy, method-level security annotations,
 * and role-based route protection into the template.
 */
@Component
public class RBACModule implements FeatureModule {

    @Override
    public FeatureToggle getSupportedToggle() {
        return FeatureToggle.RBAC;
    }

    @Override
    public boolean isCompatibleWith(ServiceType type, ServiceVariant variant) {
        return type == ServiceType.AUTH;
    }

    @Override
    public List<FeatureToggle> getDependencies() {
        return List.of(); // RBAC is standalone for AUTH
    }

    @Override
    public void apply(TemplateCompositionContext context) {

        // ─── Service: Default role assignment ──────────────────────
        context.setPlaceholder("RBAC_ANNOTATIONS", """
                            .role(Role.USER) // Default role for new registrations
                """);

        // ─── Security: Method-level security ───────────────────────
        context.addImport("org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity");
        context.setPlaceholder("RBAC_METHOD_SECURITY",
                "@EnableMethodSecurity(prePostEnabled = true)");

        // ─── Security: Role-based route protection ─────────────────
        context.setPlaceholder("RBAC_ROLE_HIERARCHY", """
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/moderator/**").hasAnyRole("ADMIN", "MODERATOR")
                """);

        // ─── Architecture ──────────────────────────────────────────
        context.addArchitectureStep(
                "→ RBAC: Role hierarchy enforced at both route and method level");
        context.addArchitectureStep(
                "→ RBAC: Roles stored in JWT claims for stateless authorization");

        context.appendToPlaceholder("ARCHITECTURE_EXTENSIONS", """
                
                **RBAC (Role-Based Access Control):**
                - Roles: ADMIN, MODERATOR, USER (extensible enum)
                - Route-level protection via SecurityFilterChain
                - Method-level protection via @PreAuthorize annotations
                - Roles embedded in JWT claims — no DB lookup needed per request
                """);

        // ─── Components & Stack ────────────────────────────────────
        context.addComponent("Role (Enum)");
        context.addComponent("@PreAuthorize Guards");
        context.addComponent("RoleHierarchy Bean");
    }
}
