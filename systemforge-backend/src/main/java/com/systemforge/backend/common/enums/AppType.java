package com.systemforge.backend.common.enums;

/**
 * Application domain type — drives template selection and architecture recommendations.
 *
 * <p>The recommendation engine maps these to curated system bundles.
 * For example: RIDE_HAILING → Auth(OTP) + Payment(Razorpay) + Notification(SMS) + Mapping.
 */
public enum AppType {

    RIDE_HAILING,
    ECOMMERCE,
    SAAS,
    FINTECH,
    HEALTHCARE,
    SOCIAL_MEDIA,
    FOOD_DELIVERY,
    EDTECH,
    IOT_PLATFORM,
    MARKETPLACE,
    ENTERPRISE_INTERNAL,
    CUSTOM
}