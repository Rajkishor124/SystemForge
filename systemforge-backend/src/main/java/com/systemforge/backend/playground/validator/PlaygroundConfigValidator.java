package com.systemforge.backend.playground.validator;

import com.systemforge.backend.playground.dto.PlaygroundConfigRequest;
import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.exception.IncompatibleFeatureException;
import com.systemforge.backend.playground.exception.UnsupportedServiceVariantException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates playground configuration before it reaches the engine.
 *
 * <p>Validation is deliberately separated from the engine so that:
 * <ul>
 *   <li>The engine remains stateless and focused on generation</li>
 *   <li>Validation rules can evolve independently</li>
 *   <li>Error messages are user-facing (not engine internals)</li>
 * </ul>
 */
@Component
public class PlaygroundConfigValidator {

    /**
     * Validates the full configuration request.
     *
     * @throws UnsupportedServiceVariantException if variant doesn't match service type
     * @throws IncompatibleFeatureException if any feature is incompatible or has missing deps
     */
    public void validate(PlaygroundConfigRequest request) {
        validateVariantBelongsToType(request);
        validateFeatureCompatibility(request);
        validateFeatureDependencies(request);
    }

    private void validateVariantBelongsToType(PlaygroundConfigRequest request) {
        if (!request.getVariant().belongsTo(request.getServiceType())) {
            throw new UnsupportedServiceVariantException(
                    request.getServiceType(), request.getVariant()
            );
        }
    }

    private void validateFeatureCompatibility(PlaygroundConfigRequest request) {
        if (request.getFeatures() == null) return;

        for (FeatureToggle feature : request.getFeatures()) {
            if (!feature.isCompatibleWith(request.getServiceType())) {
                throw new IncompatibleFeatureException(feature, request.getServiceType());
            }
        }
    }

    private void validateFeatureDependencies(PlaygroundConfigRequest request) {
        if (request.getFeatures() == null) return;

        Set<FeatureToggle> enabledSet = new HashSet<>(request.getFeatures());

        for (FeatureToggle feature : request.getFeatures()) {
            for (FeatureToggle dep : feature.getDependencies()) {
                if (!enabledSet.contains(dep)) {
                    throw new IncompatibleFeatureException(feature, dep);
                }
            }
        }
    }
}
