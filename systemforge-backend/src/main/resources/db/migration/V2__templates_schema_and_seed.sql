-- ============================================================
-- SystemForge — V2 Templates Schema + Seed Data
-- ============================================================

CREATE TABLE IF NOT EXISTS templates (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    name              VARCHAR(150)    NOT NULL,
    description       TEXT            NOT NULL,
    app_type          VARCHAR(50)     NOT NULL,
    system_type       VARCHAR(50)     NOT NULL,
    app_scale         VARCHAR(20),
    config_json       TEXT            NOT NULL,
    is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order        INTEGER         NOT NULL DEFAULT 100,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP(6)    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP(6)    NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_templates PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_tmpl_app_type ON templates (app_type);
CREATE INDEX IF NOT EXISTS idx_tmpl_system_type ON templates (system_type);
CREATE INDEX IF NOT EXISTS idx_tmpl_scale ON templates (app_scale);

-- ─── SEED TEMPLATES ──────────────────────────────────────────────────────────

INSERT INTO templates (id, name, description, app_type, system_type, app_scale, config_json, sort_order, created_at, updated_at, created_by)
VALUES
-- ECOMMERCE
(gen_random_uuid(), 'E-Commerce Microservices',
 'Production-grade e-commerce backend with product catalog, cart, checkout, payment processing, and order management. Includes search, inventory tracking, and event-driven notifications.',
 'ECOMMERCE', 'POSTGRESQL', 'MEDIUM',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","Elasticsearch","Kafka","Stripe"],"modules":["Product Catalog","Cart & Checkout","Payment Gateway","Order Management","Inventory","Search","Notifications"],"architecture":"Microservices","deployment":"Kubernetes","estimatedCost":"$200-500/mo"}',
 10, now(), now(), 'SYSTEM'),

(gen_random_uuid(), 'E-Commerce Starter (MVP)',
 'Lean e-commerce backend for startups. Modular monolith with product listing, cart, and Stripe payments. Easy to extend as you grow.',
 'ECOMMERCE', 'POSTGRESQL', 'SMALL',
 '{"techStack":["Spring Boot","PostgreSQL","Redis"],"modules":["Product Catalog","Cart","Stripe Payments","User Auth"],"architecture":"Modular Monolith","deployment":"Docker Compose","estimatedCost":"$20-50/mo"}',
 11, now(), now(), 'SYSTEM'),

-- RIDE_HAILING
(gen_random_uuid(), 'Ride-Hailing Platform',
 'Uber/Ola-style backend with real-time location tracking, driver-rider matching algorithm, dynamic pricing, payment processing, and push notifications.',
 'RIDE_HAILING', 'POSTGRESQL', 'LARGE',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","Kafka","Firebase","Razorpay"],"modules":["Auth (OTP)","Real-time Location","Matching Engine","Dynamic Pricing","Payments","Push Notifications","Trip Management"],"architecture":"Microservices","deployment":"Kubernetes","estimatedCost":"$500-1500/mo"}',
 20, now(), now(), 'SYSTEM'),

(gen_random_uuid(), 'Ride-Hailing MVP',
 'Minimal ride-sharing backend with OTP auth, basic matching, trip tracking, and payment integration. Perfect for validating your ride-sharing concept.',
 'RIDE_HAILING', 'POSTGRESQL', 'SMALL',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","Firebase"],"modules":["OTP Auth","Basic Matching","Trip Tracking","Razorpay Payments"],"architecture":"Modular Monolith","deployment":"Docker Compose","estimatedCost":"$30-80/mo"}',
 21, now(), now(), 'SYSTEM'),

-- SAAS
(gen_random_uuid(), 'Multi-Tenant SaaS Platform',
 'Enterprise SaaS backend with multi-tenancy, subscription billing, role-based access control, API rate limiting, and analytics dashboard. Supports both B2B and B2C models.',
 'SAAS', 'POSTGRESQL', 'MEDIUM',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","Stripe","RabbitMQ"],"modules":["Multi-Tenant Auth","Subscription Billing","RBAC","API Gateway","Rate Limiting","Analytics","Webhook System"],"architecture":"Modular Monolith","deployment":"Docker + Managed DB","estimatedCost":"$100-300/mo"}',
 30, now(), now(), 'SYSTEM'),

-- FINTECH
(gen_random_uuid(), 'FinTech Payment Platform',
 'Secure financial services backend with KYC verification, multi-currency wallet, P2P transfers, transaction ledger, and regulatory compliance. Built for PCI-DSS compliance.',
 'FINTECH', 'POSTGRESQL', 'MEDIUM',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","Kafka","Razorpay/Stripe"],"modules":["KYC/Identity","Digital Wallet","P2P Transfers","Transaction Ledger","Fraud Detection","Compliance Reporting"],"architecture":"Microservices","deployment":"Kubernetes","estimatedCost":"$300-800/mo"}',
 40, now(), now(), 'SYSTEM'),

-- FOOD_DELIVERY
(gen_random_uuid(), 'Food Delivery Platform',
 'Swiggy/Zomato-style backend with restaurant management, menu system, real-time order tracking, delivery assignment, and payment processing.',
 'FOOD_DELIVERY', 'POSTGRESQL', 'MEDIUM',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","Kafka","Firebase","Razorpay"],"modules":["Restaurant Management","Menu & Catalog","Order System","Delivery Tracking","Payment Processing","Push Notifications","Rating & Reviews"],"architecture":"Microservices","deployment":"Kubernetes","estimatedCost":"$200-600/mo"}',
 50, now(), now(), 'SYSTEM'),

-- SOCIAL_MEDIA
(gen_random_uuid(), 'Social Media Platform',
 'Community platform with user profiles, posts/feed, real-time chat, media uploads, notifications, and content moderation. Supports follower/following model.',
 'SOCIAL_MEDIA', 'POSTGRESQL', 'LARGE',
 '{"techStack":["Spring Boot","PostgreSQL","MongoDB","Redis","Kafka","S3","Elasticsearch"],"modules":["User Profiles","Feed Engine","Real-time Chat","Media Upload","Notifications","Content Moderation","Search"],"architecture":"Microservices","deployment":"Kubernetes","estimatedCost":"$400-1000/mo"}',
 60, now(), now(), 'SYSTEM'),

-- EDTECH
(gen_random_uuid(), 'EdTech Learning Platform',
 'Online learning backend with course management, video streaming integration, progress tracking, quizzes, certificates, and subscription plans.',
 'EDTECH', 'POSTGRESQL', 'SMALL',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","S3","Stripe"],"modules":["Course Management","Video Integration","Progress Tracking","Quiz Engine","Certificates","Subscription Billing"],"architecture":"Modular Monolith","deployment":"Docker Compose","estimatedCost":"$40-100/mo"}',
 70, now(), now(), 'SYSTEM'),

-- HEALTHCARE
(gen_random_uuid(), 'Healthcare / Telemedicine',
 'HIPAA-ready telemedicine backend with patient records, appointment scheduling, video consultation integration, prescription management, and secure messaging.',
 'HEALTHCARE', 'POSTGRESQL', 'MEDIUM',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","S3","Firebase"],"modules":["Patient Records (EHR)","Appointment Scheduling","Video Consultation","Prescription Management","Secure Messaging","Billing"],"architecture":"Modular Monolith","deployment":"Docker + Managed DB","estimatedCost":"$150-400/mo"}',
 80, now(), now(), 'SYSTEM'),

-- IOT_PLATFORM
(gen_random_uuid(), 'IoT Device Management',
 'IoT platform backend with device registration, telemetry ingestion, real-time dashboards, alerting, OTA updates, and time-series data storage.',
 'IOT_PLATFORM', 'POSTGRESQL', 'MEDIUM',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","Kafka","S3"],"modules":["Device Registry","Telemetry Ingestion","Real-time Dashboard","Alert Engine","OTA Updates","Time-series Storage"],"architecture":"Event-Driven","deployment":"Kubernetes","estimatedCost":"$200-500/mo"}',
 90, now(), now(), 'SYSTEM'),

-- MARKETPLACE
(gen_random_uuid(), 'Multi-Vendor Marketplace',
 'Amazon/Etsy-style marketplace with vendor onboarding, product listings, search, cart, split payments, reviews, and dispute resolution.',
 'MARKETPLACE', 'POSTGRESQL', 'MEDIUM',
 '{"techStack":["Spring Boot","PostgreSQL","Redis","Elasticsearch","Stripe Connect","Kafka"],"modules":["Vendor Onboarding","Product Listings","Search & Filters","Cart & Checkout","Split Payments","Reviews & Ratings","Dispute Resolution"],"architecture":"Microservices","deployment":"Kubernetes","estimatedCost":"$250-700/mo"}',
 100, now(), now(), 'SYSTEM');
