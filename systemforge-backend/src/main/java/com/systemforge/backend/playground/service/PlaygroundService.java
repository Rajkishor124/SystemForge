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

    /** Get the authenticated user's generation history */
    List<PlaygroundGeneratedOutput> getHistory();
}
