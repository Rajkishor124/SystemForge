package com.systemforge.backend.recommendation.model;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.common.enums.FeatureType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Internal domain model used by Recommendation Engine.
 *
 * <p>This is the "brain input" of the system.
 * It abstracts away external DTOs and provides a clean,
 * rule-friendly structure for decision making.
 */
public final class ProjectContext {

    private final AppType appType;
    private final AppScale appScale;
    private final Set<FeatureType> features;

    private final String region;
    private final Integer expectedUsers;

    // ========================
    // Constructor (Private)
    // ========================

    private ProjectContext(Builder builder) {
        this.appType = Objects.requireNonNull(builder.appType, "AppType must not be null");
        this.appScale = Objects.requireNonNull(builder.appScale, "AppScale must not be null");

        this.features = Collections.unmodifiableSet(
                builder.features != null ? new HashSet<>(builder.features) : new HashSet<>()
        );

        this.region = builder.region;
        this.expectedUsers = builder.expectedUsers;
    }

    // ========================
    // Builder
    // ========================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AppType appType;
        private AppScale appScale;
        private Set<FeatureType> features;
        private String region;
        private Integer expectedUsers;

        public Builder appType(AppType appType) {
            this.appType = appType;
            return this;
        }

        public Builder appScale(AppScale appScale) {
            this.appScale = appScale;
            return this;
        }

        public Builder features(Set<FeatureType> features) {
            this.features = features;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder expectedUsers(Integer expectedUsers) {
            this.expectedUsers = expectedUsers;
            return this;
        }

        public ProjectContext build() {
            return new ProjectContext(this);
        }
    }

    // ========================
    // Getters
    // ========================

    public AppType getAppType() {
        return appType;
    }

    public AppScale getAppScale() {
        return appScale;
    }

    public Set<FeatureType> getFeatures() {
        return features;
    }

    public String getRegion() {
        return region;
    }

    public Integer getExpectedUsers() {
        return expectedUsers;
    }

    // ========================
    // Helper Methods (VERY IMPORTANT)
    // ========================

    /**
     * Check if a feature is requested.
     */
    public boolean hasFeature(FeatureType feature) {
        return features.contains(feature);
    }

    /**
     * Check if app is of a specific type.
     */
    public boolean isAppType(AppType type) {
        return this.appType == type;
    }

    /**
     * Check scale conditions.
     */
    public boolean isSmallScale() {
        return appScale == AppScale.SMALL;
    }

    public boolean isMediumScale() {
        return appScale == AppScale.MEDIUM;
    }

    public boolean isLargeScale() {
        return appScale == AppScale.LARGE;
    }
}