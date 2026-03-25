package com.systemforge.backend.common.enums;

/**
 * Infrastructure-level systems (HOW things are implemented).
 */
public enum SystemType {

    // DATABASE
    MYSQL,
    POSTGRESQL,
    MONGODB,
    AURORA,

    // CACHING
    REDIS,

    // MESSAGING
    KAFKA,
    RABBITMQ,
    SQS,

    // SEARCH
    ELASTICSEARCH,

    // STORAGE
    S3,

    // CDN
    CLOUDFRONT,

    // NOTIFICATION INFRA
    FIREBASE,
    SMTP,

    // PAYMENT GATEWAYS
    RAZORPAY,
    STRIPE

}