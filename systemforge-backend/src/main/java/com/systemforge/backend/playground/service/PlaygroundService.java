package com.systemforge.backend.playground.service;

import com.systemforge.backend.playground.dto.PlaygroundConfigRequest;
import com.systemforge.backend.playground.dto.PlaygroundGeneratedOutput;
import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;

import java.util.List;

/**
 * Service contract for the Microservices Playground.
 */
public interface PlaygroundService {

    /** List all available service types */
    List<ServiceType> getServiceTypes();

    /** List variants for a given service type */
    List<ServiceVariant> getVariants(ServiceType type);

    /** List compatible feature toggles for a type + variant */
    List<FeatureToggle> getFeatures(ServiceType type, ServiceVariant variant);

    /** Generate an architecture from config (cached + persisted) */
    PlaygroundGeneratedOutput generate(PlaygroundConfigRequest request);

    /**
     * Retrieves the generation history for the authenticated user.
     */
    List<PlaygroundGeneratedOutput> getHistory();

    /**
     * Generates and bundles the configuration into a runnable Spring Boot ZIP project.
     */
    byte[] exportZip(PlaygroundConfigRequest request);
}
