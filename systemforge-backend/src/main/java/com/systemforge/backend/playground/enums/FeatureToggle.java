package com.systemforge.backend.playground.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Feature toggles that can be applied to a service configuration.
 *
 * <p>Each toggle declares:
 * <ul>
 *   <li>{@code compatibleTypes} — which ServiceTypes it can work with</li>
 *   <li>{@code dependencies} — other toggles that must be applied first</li>
 * </ul>
 *
 * <p>The engine uses these declarations for validation and topological ordering.
 */
@Getter
@RequiredArgsConstructor
public enum FeatureToggle {

    REFRESH_TOKEN(
            Set.of(ServiceType.AUTH),
            List.of()
    ),
    RBAC(
            Set.of(ServiceType.AUTH),
            List.of() // RBAC is standalone for AUTH
    ),
    TWO_FACTOR_AUTH(
            Set.of(ServiceType.AUTH),
            List.of() // Future: could depend on NOTIFICATION
    ),
    RATE_LIMITING(
            Set.of(ServiceType.AUTH, ServiceType.PAYMENT, ServiceType.NOTIFICATION,
                    ServiceType.DATABASE, ServiceType.STORAGE, ServiceType.MESSAGING),
            List.of()
    ),
    AUDIT_LOGGING(
            Set.of(ServiceType.AUTH, ServiceType.PAYMENT, ServiceType.NOTIFICATION,
                    ServiceType.DATABASE, ServiceType.STORAGE, ServiceType.MESSAGING),
            List.of()
    ),
    IP_WHITELISTING(
            Set.of(ServiceType.AUTH, ServiceType.PAYMENT),
            List.of()
    );

    private final Set<ServiceType> compatibleTypes;
    private final List<FeatureToggle> dependencies;

    /**
     * Checks if this toggle is valid for the given service type.
     */
    public boolean isCompatibleWith(ServiceType type) {
        return compatibleTypes.contains(type);
    }
}
