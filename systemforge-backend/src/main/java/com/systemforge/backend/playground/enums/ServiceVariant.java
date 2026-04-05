package com.systemforge.backend.playground.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Specific implementation variants within a {@link ServiceType}.
 *
 * <p>Each variant is bound to exactly one parent ServiceType.
 * The engine uses this mapping to validate configs and resolve templates.
 *
 * <p>The template key convention is: {@code PARENT_VARIANT} (e.g., {@code AUTH_JWT}).
 */
@Getter
@RequiredArgsConstructor
public enum ServiceVariant {

    // ─── AUTH variants ─────────────────────────────────────────────
    JWT(ServiceType.AUTH),
    OAUTH2(ServiceType.AUTH),
    OTP(ServiceType.AUTH),
    SESSION(ServiceType.AUTH),

    // ─── PAYMENT variants ──────────────────────────────────────────
    RAZORPAY(ServiceType.PAYMENT),
    STRIPE(ServiceType.PAYMENT),
    UPI(ServiceType.PAYMENT);

    private final ServiceType parentType;

    /**
     * Returns the template registry key for this variant.
     * Convention: {@code AUTH_JWT}, {@code PAYMENT_STRIPE}, etc.
     */
    public String toTemplateKey() {
        return parentType.name() + "_" + this.name();
    }

    /**
     * Checks if this variant belongs to the given service type.
     */
    public boolean belongsTo(ServiceType type) {
        return this.parentType == type;
    }
}
