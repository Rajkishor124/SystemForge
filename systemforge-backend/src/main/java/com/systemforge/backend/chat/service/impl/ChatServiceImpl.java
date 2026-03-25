package com.systemforge.backend.chat.service.impl;

import com.systemforge.backend.chat.dto.ChatRequest;
import com.systemforge.backend.chat.dto.ChatResponse;
import com.systemforge.backend.chat.service.ChatService;
import com.systemforge.backend.recommendation.ai.client.OpenAiClientAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired(required = false)
    private OpenAiClientAdapter openAiClientAdapter;

    // ─── Rule-based response patterns for system architecture topics ──────────

    private static final Map<Pattern, String> RULE_RESPONSES = Map.ofEntries(
            Map.entry(Pattern.compile("(?i).*\\b(database|db|sql|nosql|postgres|mysql|mongo)\\b.*"),
                    """
                    ## Database Recommendations

                    The choice depends on your data model and access patterns:

                    **Relational (PostgreSQL recommended)**
                    - Structured data with relationships (users, orders, payments)
                    - ACID compliance needed
                    - Complex queries and joins

                    **Document Store (MongoDB)**
                    - Flexible schemas, nested documents
                    - Content management, catalogs, logs

                    **In-Memory (Redis)**
                    - Caching, session storage, rate limiting
                    - Real-time leaderboards, pub/sub messaging
                    - Geospatial queries (driver matching)

                    **Key recommendations:**
                    - Start with PostgreSQL — it handles 90% of use cases
                    - Add Redis as a caching layer when latency matters
                    - Only add MongoDB if you have genuinely unstructured data

                    Would you like me to design a specific schema for your use case?
                    """),

            Map.entry(Pattern.compile("(?i).*\\b(scale|scaling|performance|high.?availability|load|concurrent)\\b.*"),
                    """
                    ## Scaling Strategy

                    **Horizontal Scaling (recommended for most apps)**
                    1. **Load Balancer** → Distribute traffic (Nginx, AWS ALB)
                    2. **Read Replicas** → Scale database reads
                    3. **Caching Layer** → Redis for hot data (80/20 rule)
                    4. **Queue-based Processing** → Async for heavy work (Kafka/RabbitMQ)

                    **Vertical Scaling (quick wins)**
                    - Upgrade DB instance size
                    - Connection pooling (PgBouncer)
                    - Query optimization and indexing

                    **At Enterprise Scale (500k+ users)**
                    - Microservices extraction for independent scaling
                    - Kubernetes for container orchestration
                    - Multi-region deployment
                    - CDN for static assets

                    What's your current traffic profile and bottleneck?
                    """),

            Map.entry(Pattern.compile("(?i).*\\b(auth|authentication|login|jwt|oauth|security|token|password)\\b.*"),
                    """
                    ## Authentication Architecture

                    **Recommended: JWT + Refresh Token Pattern**

                    ```
                    Client → Login → Server validates → Returns:
                      - Access Token (short-lived, 15-30 min)
                      - Refresh Token (long-lived, 7-30 days)
                    ```

                    **Best Practices:**
                    - Store access token in memory (not localStorage)
                    - Store refresh token as HttpOnly cookie
                    - Use BCrypt for password hashing (cost factor 12)
                    - Implement rate limiting on auth endpoints
                    - Add OTP/2FA for sensitive operations

                    **For Social Login:** Use OAuth2 with providers (Google, GitHub)
                    **For Mobile Apps:** OTP-based authentication is more user-friendly

                    Want me to design the full auth flow for your specific app type?
                    """),

            Map.entry(Pattern.compile("(?i).*\\b(microservice|monolith|architect|pattern|design|modular)\\b.*"),
                    """
                    ## Architecture Pattern Selection

                    **Start with Modular Monolith** (recommended for MVPs):
                    - Faster development, easier debugging
                    - Clear module boundaries (auth, payments, etc.)
                    - Can extract to microservices later

                    **Move to Microservices when:**
                    - Different modules need independent scaling
                    - Teams are large enough (2+ teams)
                    - Deployment independence is critical

                    **Event-Driven Architecture:**
                    - Best for real-time systems (chat, notifications, IoT)
                    - Use Kafka for event streaming
                    - Eventual consistency is acceptable

                    **CQRS Pattern:**
                    - Separate read and write models
                    - Best for high read-to-write ratio apps
                    - Pairs well with Event Sourcing

                    What's the complexity and team size of your project?
                    """),

            Map.entry(Pattern.compile("(?i).*\\b(api|rest|grpc|graphql|endpoint)\\b.*"),
                    """
                    ## API Design Recommendations

                    **REST API (default choice)**
                    - Simple, widely understood
                    - Best for CRUD operations
                    - Use OpenAPI/Swagger for docs

                    **GraphQL**
                    - Client-specified queries (no over/under-fetching)
                    - Best for complex frontend data needs
                    - Higher backend complexity

                    **gRPC**
                    - High performance (Protocol Buffers)
                    - Best for internal service-to-service calls
                    - Bi-directional streaming support

                    **Best Practice:**
                    REST for public APIs, gRPC for internal services, WebSocket for real-time.

                    What type of clients will consume your API?
                    """),

            Map.entry(Pattern.compile("(?i).*\\b(cache|redis|memcache|cdn)\\b.*"),
                    """
                    ## Caching Strategy

                    **Layer 1 — Application Cache (Redis)**
                    - Session data, user profiles, config
                    - TTL: 5-30 minutes depending on data freshness needs
                    - Pattern: Cache-aside (read-through)

                    **Layer 2 — Query Cache**
                    - Frequently accessed DB queries
                    - Invalidate on write operations

                    **Layer 3 — CDN**
                    - Static assets (images, JS, CSS)
                    - API response caching for public endpoints

                    **Cache Invalidation Strategies:**
                    - TTL-based (simplest)
                    - Event-based (publish cache invalidation events)
                    - Write-through (update cache on every write)

                    What specific data are you looking to cache?
                    """),

            Map.entry(Pattern.compile("(?i).*\\b(deploy|docker|kubernetes|k8s|ci.?cd|devops|cloud|aws|gcp|azure)\\b.*"),
                    """
                    ## Deployment & Infrastructure

                    **For Startups (cost-effective)**
                    - Docker Compose on a single VPS
                    - Managed database (RDS/Cloud SQL)
                    - GitHub Actions for CI/CD

                    **For Growth Stage**
                    - Kubernetes (EKS/GKE) for container orchestration
                    - Terraform for infrastructure as code
                    - Blue-green or canary deployments

                    **For Enterprise**
                    - Multi-region deployment
                    - Service mesh (Istio)
                    - Centralized logging (ELK/Datadog)
                    - Distributed tracing (Jaeger/Zipkin)

                    What's your current infrastructure and budget?
                    """),

            Map.entry(Pattern.compile("(?i).*\\b(queue|kafka|rabbit|sqs|message|event|async)\\b.*"),
                    """
                    ## Message Queue & Event Streaming

                    **Apache Kafka**
                    - Best for: event streaming, log aggregation, high throughput
                    - Pros: replay events, massive scale
                    - Cons: more complex to operate

                    **RabbitMQ**
                    - Best for: task queues, request/reply, routing
                    - Pros: simpler setup, flexible routing
                    - Cons: not ideal for replay/streaming

                    **AWS SQS**
                    - Best for: simple async processing, serverless
                    - Pros: zero maintenance, pay-per-use
                    - Cons: limited to AWS ecosystem

                    **When to use a queue:**
                    - Email/SMS notifications
                    - Order processing pipelines
                    - Background jobs (reports, exports)
                    - Decoupling services

                    What specific async workflow do you need to implement?
                    """),

            Map.entry(Pattern.compile("(?i).*\\b(payment|stripe|razorpay|billing|subscription)\\b.*"),
                    """
                    ## Payment System Architecture

                    **Recommended: Stripe or Razorpay (India)**

                    **Core Components:**
                    1. **Payment Gateway Integration** — Stripe/Razorpay SDK
                    2. **Webhook Processor** — Async payment confirmation
                    3. **Idempotency Layer** — Prevent duplicate charges
                    4. **Ledger Service** — Transaction history & reconciliation

                    **Best Practices:**
                    - Never store card details (use tokenization)
                    - Always use webhooks for payment confirmation (not client-side)
                    - Implement idempotency keys for every payment request
                    - Keep a separate ledger table for audit trail
                    - Use database transactions for balance updates

                    What payment model do you need? (one-time, subscription, marketplace)
                    """)
    );

    private static final String DEFAULT_RESPONSE = """
            That's a great question about system design! I can help you with:

            - **Architecture patterns** — Monolith vs Microservices vs Event-Driven
            - **Database selection** — SQL vs NoSQL vs Hybrid approaches
            - **Scaling strategies** — Horizontal scaling, caching, CDN
            - **Authentication** — JWT, OAuth2, OTP-based flows
            - **API design** — REST, gRPC, GraphQL best practices
            - **Infrastructure** — Docker, Kubernetes, CI/CD pipelines
            - **Message queues** — Kafka, RabbitMQ, SQS
            - **Payment systems** — Stripe, Razorpay integration

            Could you be more specific about what aspect of system design you need help with? For example:
            - *"How should I design the database for my e-commerce app?"*
            - *"What's the best authentication strategy for a mobile app?"*
            - *"How do I scale my API to handle 100k concurrent users?"*
            """;

    @Override
    public ChatResponse chat(ChatRequest request) {
        String message = request.getMessage();

        // Try OpenAI-powered response if adapter is available
        if (openAiClientAdapter != null) {
            try {
                String prompt = buildPrompt(request);
                // Use the existing adapter for a simple text completion
                // This leverages the structured completion approach
                String reply = openAiClientAdapter.getStructuredCompletion(prompt, String.class);
                return ChatResponse.builder()
                        .reply(reply)
                        .source("AI")
                        .build();
            } catch (Exception e) {
                log.warn("OpenAI chat failed, falling back to rule engine: {}", e.getMessage());
            }
        }

        // Fall back to rule-based responses
        return chatWithRuleEngine(message);
    }

    private String buildPrompt(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are SystemForge AI — an expert system design assistant. ");
        sb.append("Provide concise, practical architecture advice using markdown formatting.\n\n");

        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            sb.append("Previous conversation:\n");
            for (ChatRequest.ChatMessage msg : request.getHistory()) {
                sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("User: ").append(request.getMessage());
        return sb.toString();
    }

    private ChatResponse chatWithRuleEngine(String message) {
        log.debug("Generating chat response via rule engine");

        for (Map.Entry<Pattern, String> entry : RULE_RESPONSES.entrySet()) {
            if (entry.getKey().matcher(message).matches()) {
                return ChatResponse.builder()
                        .reply(entry.getValue().trim())
                        .source("RULE_ENGINE")
                        .build();
            }
        }

        return ChatResponse.builder()
                .reply(DEFAULT_RESPONSE.trim())
                .source("RULE_ENGINE")
                .build();
    }
}
