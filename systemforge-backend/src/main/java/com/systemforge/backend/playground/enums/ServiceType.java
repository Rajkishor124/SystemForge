package com.systemforge.backend.playground.enums;

/**
 * Supported microservice domains in the Playground.
 *
 * <p>Each value represents a top-level service category that
 * maps to one or more {@link ServiceVariant}s.
 *
 * <p>Extensibility: Add a new value here + register a matching
 * {@link com.systemforge.backend.playground.template.ServiceTemplate} bean.
 * Zero engine changes required.
 */
public enum ServiceType {

    AUTH,
    PAYMENT,
    NOTIFICATION,
    DATABASE,
    STORAGE,
    MESSAGING
}
