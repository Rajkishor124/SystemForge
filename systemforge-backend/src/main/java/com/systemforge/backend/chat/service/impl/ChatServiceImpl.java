package com.systemforge.backend.chat.service.impl;

import com.systemforge.backend.auth.util.SecurityPrincipalUtil;
import com.systemforge.backend.chat.dto.*;
import com.systemforge.backend.chat.entity.ChatMessageEntity;
import com.systemforge.backend.chat.entity.Conversation;
import com.systemforge.backend.chat.repository.ChatMessageRepository;
import com.systemforge.backend.chat.repository.ConversationRepository;
import com.systemforge.backend.chat.service.ChatService;
import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.recommendation.ai.client.OpenAiClientAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@SuppressWarnings("null")
public class ChatServiceImpl implements ChatService {

    @Autowired(required = false)
    private OpenAiClientAdapter openAiClientAdapter;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

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

    // ─── Legacy chat endpoint (backward compatibility) ─────────────────────────

    @Override
    public ChatResponse chat(ChatRequest request) {
        String message = request.getMessage();

        if (openAiClientAdapter != null) {
            try {
                String prompt = buildPromptFromRequest(request);
                String reply = openAiClientAdapter.getStructuredCompletion(prompt, String.class);
                return ChatResponse.builder().reply(reply).source("AI").build();
            } catch (Exception e) {
                log.warn("OpenAI chat failed, falling back to rule engine: {}", e.getMessage());
            }
        }

        return chatWithRuleEngine(message);
    }

    // ─── Conversation CRUD ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public ConversationDto createConversation() {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();

        Conversation conversation = Conversation.builder()
                .userId(userId)
                .title("New Conversation")
                .build();

        conversation = conversationRepository.save(conversation);

        return toDto(conversation, 0, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> listConversations() {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return conversations.stream().map(conv -> {
            List<ChatMessageEntity> messages = chatMessageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conv.getId());
            String lastPreview = null;
            if (!messages.isEmpty()) {
                ChatMessageEntity last = messages.get(messages.size() - 1);
                lastPreview = last.getContent().length() > 80
                        ? last.getContent().substring(0, 80) + "..."
                        : last.getContent();
            }
            return toDto(conv, messages.size(), lastPreview);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailDto getConversation(UUID conversationId) {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("CONV_NOT_FOUND", "Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("CONV_ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN);
        }

        List<ChatMessageEntity> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        return ConversationDetailDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messages(messages.stream().map(this::toMessageDto).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public ChatMessageDto sendMessage(UUID conversationId, SendMessageRequest request) {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("CONV_NOT_FOUND", "Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("CONV_ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN);
        }

        // Save user message
        ChatMessageEntity userMsg = ChatMessageEntity.builder()
                .conversationId(conversationId)
                .role("user")
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMsg);

        // Auto-title from first user message
        List<ChatMessageEntity> existingMessages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
        long userMessageCount = existingMessages.stream().filter(m -> "user".equals(m.getRole())).count();
        if (userMessageCount <= 1) {
            String autoTitle = request.getMessage().length() > 50
                    ? request.getMessage().substring(0, 50) + "..."
                    : request.getMessage();
            conversation.setTitle(autoTitle);
            conversationRepository.save(conversation);
        }

        // Generate AI response
        ChatResponse aiResponse = generateAiResponse(request.getMessage(), existingMessages);

        // Save AI message
        ChatMessageEntity aiMsg = ChatMessageEntity.builder()
                .conversationId(conversationId)
                .role("assistant")
                .content(aiResponse.getReply())
                .source(aiResponse.getSource())
                .build();
        aiMsg = chatMessageRepository.save(aiMsg);

        return toMessageDto(aiMsg);
    }

    @Override
    @Transactional
    public ConversationDto renameConversation(UUID conversationId, RenameConversationRequest request) {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("CONV_NOT_FOUND", "Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("CONV_ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN);
        }

        conversation.setTitle(request.getTitle());
        conversation = conversationRepository.save(conversation);
        return toDto(conversation, 0, null);
    }

    @Override
    @Transactional
    public void deleteConversation(UUID conversationId) {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("CONV_NOT_FOUND", "Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("CONV_ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN);
        }

        conversation.setDeleted(true);
        conversationRepository.save(conversation);
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private ChatResponse generateAiResponse(String message, List<ChatMessageEntity> history) {
        if (openAiClientAdapter != null) {
            try {
                String prompt = buildPromptFromHistory(message, history);
                String reply = openAiClientAdapter.getStructuredCompletion(prompt, String.class);
                return ChatResponse.builder().reply(reply).source("AI").build();
            } catch (Exception e) {
                log.warn("OpenAI chat failed, falling back to rule engine: {}", e.getMessage());
            }
        }
        return chatWithRuleEngine(message);
    }

    private String buildPromptFromHistory(String currentMessage, List<ChatMessageEntity> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are SystemForge AI — an expert system design assistant. ");
        sb.append("Provide concise, practical architecture advice using markdown formatting.\n\n");

        // Include last 10 messages for context
        List<ChatMessageEntity> recent = history.size() > 10
                ? history.subList(history.size() - 10, history.size())
                : history;

        if (!recent.isEmpty()) {
            sb.append("Previous conversation:\n");
            for (ChatMessageEntity msg : recent) {
                sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("User: ").append(currentMessage);
        return sb.toString();
    }

    private String buildPromptFromRequest(ChatRequest request) {
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

    private ConversationDto toDto(Conversation conv, int messageCount, String lastPreview) {
        return ConversationDto.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .messageCount(messageCount)
                .lastMessagePreview(lastPreview)
                .build();
    }

    private ChatMessageDto toMessageDto(ChatMessageEntity msg) {
        return ChatMessageDto.builder()
                .id(msg.getId())
                .role(msg.getRole())
                .content(msg.getContent())
                .source(msg.getSource())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
