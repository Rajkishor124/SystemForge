-- ============================================================
-- SystemForge — V4 Add default_prompt to templates
-- ============================================================

ALTER TABLE templates ADD COLUMN IF NOT EXISTS default_prompt TEXT;

-- ─── UPDATE EXISTING TEMPLATES WITH CURATED DEFAULT PROMPTS ─────────────────

-- E-Commerce Microservices
UPDATE templates SET default_prompt =
'I need a production-grade e-commerce backend system. It should include a product catalog with categories and search, a shopping cart with session persistence, a full checkout flow with Stripe payment integration, and an order management system with status tracking. The system needs inventory management with stock reservations, event-driven email/SMS notifications for order updates, and Elasticsearch-powered search with filters and facets. Target scale is 10k-500k concurrent users with microservices architecture deployed on Kubernetes.'
WHERE name = 'E-Commerce Microservices';

-- E-Commerce Starter (MVP)
UPDATE templates SET default_prompt =
'I need a lean e-commerce backend for my startup MVP. It should have a simple product catalog with CRUD operations, a basic shopping cart, Stripe payment integration for checkout, and user authentication with JWT. Keep it as a modular monolith that I can easily extend later. Target scale is under 10k users, deployed with Docker Compose on a single server.'
WHERE name = 'E-Commerce Starter (MVP)';

-- Ride-Hailing Platform
UPDATE templates SET default_prompt =
'I need a backend for a ride-sharing platform similar to Uber/Ola. It must handle OTP-based authentication, real-time GPS location tracking for drivers and riders, an intelligent driver-rider matching algorithm with proximity-based search, dynamic surge pricing based on demand, payment processing with Razorpay, push notifications via Firebase, and a complete trip lifecycle management system (request → match → ride → complete → rate). The system should handle 500k+ concurrent users with microservices on Kubernetes.'
WHERE name = 'Ride-Hailing Platform';

-- Ride-Hailing MVP
UPDATE templates SET default_prompt =
'I need a minimal ride-sharing backend to validate my concept. It should include OTP-based phone authentication, basic driver-rider matching by proximity, simple trip tracking with status updates, Razorpay payment integration, and Firebase push notifications. Keep it simple as a modular monolith with Docker Compose deployment. Target is under 10k users for initial launch.'
WHERE name = 'Ride-Hailing MVP';

-- Multi-Tenant SaaS Platform
UPDATE templates SET default_prompt =
'I need a multi-tenant SaaS platform backend. It should support tenant isolation at the database schema level, subscription billing with Stripe (free, pro, enterprise tiers), role-based access control (RBAC) with granular permissions, an API gateway with rate limiting per tenant, a webhook system for third-party integrations, and an analytics dashboard with usage metrics. The architecture should be a modular monolith that can scale to microservices later. Target scale is 10k-500k users.'
WHERE name = 'Multi-Tenant SaaS Platform';

-- FinTech Payment Platform
UPDATE templates SET default_prompt =
'I need a secure financial services backend built for PCI-DSS compliance. It should include KYC/identity verification workflows, a multi-currency digital wallet system, peer-to-peer money transfers with transaction guarantees, a double-entry transaction ledger for auditability, basic fraud detection rules engine, and regulatory compliance reporting. All financial operations must be ACID-compliant with full audit trails. Target scale is 10k-500k users with microservices on Kubernetes.'
WHERE name = 'FinTech Payment Platform';

-- Food Delivery Platform
UPDATE templates SET default_prompt =
'I need a food delivery platform backend like Swiggy or DoorDash. It should handle restaurant onboarding and management, dynamic menu systems with item availability, real-time order tracking from placement to delivery, delivery partner assignment with route optimization, payment processing with Razorpay, push notifications for order status updates, and a rating/review system for restaurants and delivery partners. Target scale is 10k-500k users with microservices architecture.'
WHERE name = 'Food Delivery Platform';

-- Social Media Platform
UPDATE templates SET default_prompt =
'I need a social media platform backend with user profiles and follower/following relationships, a news feed engine with algorithmic ranking, real-time chat/messaging using WebSockets, media upload and storage with S3 integration, push and in-app notifications, content moderation tools with reporting, and full-text search for users and posts via Elasticsearch. The system should support 500k+ concurrent users with event-driven microservices on Kubernetes.'
WHERE name = 'Social Media Platform';

-- EdTech Learning Platform
UPDATE templates SET default_prompt =
'I need an online learning platform backend. It should support course creation and management with modules and lessons, video content integration with streaming support via S3, student progress tracking with completion percentages, a quiz and assessment engine with auto-grading, certificate generation upon course completion, and subscription-based billing with Stripe (monthly/yearly plans). Keep it as a modular monolith for MVP. Target scale is under 10k users.'
WHERE name = 'EdTech Learning Platform';

-- Healthcare / Telemedicine
UPDATE templates SET default_prompt =
'I need a HIPAA-ready telemedicine backend. It should include secure patient health records (EHR) management, appointment scheduling with calendar integration, video consultation session management, prescription management with digital prescriptions, end-to-end encrypted messaging between doctors and patients, and a billing system for consultations and services. All data must be encrypted at rest and in transit. Target scale is 10k-500k users with a modular monolith architecture.'
WHERE name = 'Healthcare / Telemedicine';

-- IoT Device Management
UPDATE templates SET default_prompt =
'I need an IoT platform backend for device management. It should handle device registration and provisioning with unique identifiers, high-throughput telemetry data ingestion via Kafka, real-time monitoring dashboards with WebSocket updates, configurable alerting rules engine with notification channels, over-the-air (OTA) firmware update management, and time-series data storage for historical analytics. The system should be event-driven and handle 10k-500k connected devices deployed on Kubernetes.'
WHERE name = 'IoT Device Management';

-- Multi-Vendor Marketplace
UPDATE templates SET default_prompt =
'I need a multi-vendor marketplace backend like Amazon or Etsy. It should support vendor onboarding with verification workflows, product listing management with rich attributes, full-text search with filters via Elasticsearch, shopping cart and checkout with split payments (Stripe Connect), a review and rating system for products and vendors, and a dispute resolution workflow. Target scale is 10k-500k users with microservices on Kubernetes.'
WHERE name = 'Multi-Vendor Marketplace';
