package com.systemforge.backend.user.enums;

/**
 * Represents the lifecycle state of a user account.
 *
 * Replaces simple boolean flags (like isActive) with a more expressive model.
 */
public enum AccountStatus {

    /**
     * User is fully active and can access the system.
     */
    ACTIVE,

    /**
     * User is temporarily blocked by admin.
     * Cannot login or perform actions.
     */
    SUSPENDED,

    /**
     * User has deactivated their account.
     * Can be reactivated later.
     */
    DEACTIVATED
}