package com.systemforge.backend.architect.service.impl;

import com.systemforge.backend.architect.dto.AgentStep;
import com.systemforge.backend.architect.dto.ArchitectRequest;
import com.systemforge.backend.architect.dto.ArchitectResponse;
import com.systemforge.backend.architect.entity.ArchitectMessage;
import com.systemforge.backend.architect.entity.ArchitectSession;
import com.systemforge.backend.architect.entity.ArchitectStepEntity;
import com.systemforge.backend.architect.orchestrator.AgentContext;
import com.systemforge.backend.architect.orchestrator.AgentOrchestrator;
import com.systemforge.backend.architect.repository.ArchitectMessageRepository;
import com.systemforge.backend.architect.repository.ArchitectSessionRepository;
import com.systemforge.backend.architect.repository.ArchitectStepRepository;
import com.systemforge.backend.architect.service.ArchitectService;
import com.systemforge.backend.auth.util.SecurityPrincipalUtil;
import com.systemforge.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI Architect service — orchestrates agent execution with persistence.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ArchitectServiceImpl implements ArchitectService {

    private final AgentOrchestrator orchestrator;
    private final ArchitectSessionRepository sessionRepository;
    private final ArchitectMessageRepository messageRepository;
    private final ArchitectStepRepository stepRepository;

    @Override
    @Transactional
    public ArchitectResponse process(ArchitectRequest request) {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        long startTime = System.currentTimeMillis();

        // 1. Resolve or create session
        ArchitectSession session = resolveSession(request, userId);

        // 2. Save user message
        ArchitectMessage userMessage = ArchitectMessage.builder()
                .sessionId(session.getId())
                .role("user")
                .content(request.getMessage())
                .build();
        messageRepository.save(userMessage);

        // 3. Load conversation history
        List<ArchitectMessage> history = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(session.getId());

        // 4. Build agent context
        AgentContext context = new AgentContext(request.getMessage());
        context.setSessionId(session.getId());
        context.setUserId(userId);
        context.setConversationHistory(
                history.stream()
                        .map(m -> m.getRole() + ": " + m.getContent())
                        .collect(Collectors.toList())
        );

        // 5. Execute agent pipeline
        orchestrator.execute(context);

        long processingTime = System.currentTimeMillis() - startTime;

        // 6. Save assistant message
        ArchitectMessage assistantMessage = ArchitectMessage.builder()
                .sessionId(session.getId())
                .role("assistant")
                .content(context.getFinalReply())
                .source(context.getSource())
                .intent(context.getIntent())
                .processingTimeMs(processingTime)
                .build();
        assistantMessage = messageRepository.save(assistantMessage);

        // 7. Save reasoning steps
        for (AgentStep step : context.getSteps()) {
            stepRepository.save(ArchitectStepEntity.builder()
                    .messageId(assistantMessage.getId())
                    .stepName(step.getName())
                    .stepOrder(step.getOrder())
                    .status(step.getStatus())
                    .output(step.getOutput())
                    .durationMs(step.getDurationMs())
                    .build());
        }

        // 8. Update session metadata
        if (session.getIntent() == null) {
            session.setIntent(context.getIntent());
        }
        // Auto-title from first message
        long msgCount = messageRepository.countBySessionId(session.getId());
        if (msgCount <= 2) {
            String autoTitle = request.getMessage().length() > 60
                    ? request.getMessage().substring(0, 60) + "..."
                    : request.getMessage();
            session.setTitle(autoTitle);
        }
        sessionRepository.save(session);

        log.info("[ARCHITECT_SERVICE] userId={}, sessionId={}, intent={}, source={}, steps={}, timeMs={}",
                userId, session.getId(), context.getIntent(), context.getSource(),
                context.getSteps().size(), processingTime);

        // 9. Build response
        return ArchitectResponse.builder()
                .sessionId(session.getId())
                .reply(context.getFinalReply())
                .intent(context.getIntent())
                .source(context.getSource())
                .reasoningSteps(context.getSteps())
                .processingTimeMs(processingTime)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArchitectSessionDto> listSessions() {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        List<ArchitectSession> sessions = sessionRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);

        return sessions.stream().map(s -> {
            long count = messageRepository.countBySessionId(s.getId());
            List<ArchitectMessage> messages = messageRepository
                    .findBySessionIdOrderByCreatedAtAsc(s.getId());
            String preview = null;
            if (!messages.isEmpty()) {
                ArchitectMessage last = messages.get(messages.size() - 1);
                preview = last.getContent().length() > 100
                        ? last.getContent().substring(0, 100) + "..."
                        : last.getContent();
            }
            return new ArchitectSessionDto(
                    s.getId(), s.getTitle(), s.getIntent(), s.getStatus(),
                    count, preview, s.getCreatedAt(), s.getUpdatedAt()
            );
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ArchitectSessionDetailDto getSession(UUID sessionId) {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        ArchitectSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND",
                        "Session not found", HttpStatus.NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("SESSION_ACCESS_DENIED",
                    "Access denied", HttpStatus.FORBIDDEN);
        }

        List<ArchitectMessage> messages = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);

        List<ArchitectMessageDto> messageDtos = messages.stream().map(m -> {
            List<AgentStep> steps = List.of();
            if ("assistant".equals(m.getRole())) {
                steps = stepRepository.findByMessageIdOrderByStepOrderAsc(m.getId())
                        .stream()
                        .map(s -> AgentStep.builder()
                                .name(s.getStepName())
                                .order(s.getStepOrder())
                                .status(s.getStatus())
                                .output(s.getOutput())
                                .durationMs(s.getDurationMs())
                                .build())
                        .collect(Collectors.toList());
            }
            return new ArchitectMessageDto(
                    m.getId(), m.getRole(), m.getContent(), m.getSource(),
                    m.getIntent(), m.getProcessingTimeMs(), m.getCreatedAt(), steps
            );
        }).collect(Collectors.toList());

        return new ArchitectSessionDetailDto(
                session.getId(), session.getTitle(), session.getIntent(),
                session.getStatus(), session.getCreatedAt(), messageDtos
        );
    }

    @Override
    @Transactional
    public void deleteSession(UUID sessionId) {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        ArchitectSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND",
                        "Session not found", HttpStatus.NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("SESSION_ACCESS_DENIED",
                    "Access denied", HttpStatus.FORBIDDEN);
        }

        session.setDeleted(true);
        sessionRepository.save(session);
    }

    // ─── Private ───────────────────────────────────────────────────────────

    private ArchitectSession resolveSession(ArchitectRequest request, UUID userId) {
        if (request.getSessionId() != null) {
            ArchitectSession existing = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND",
                            "Session not found", HttpStatus.NOT_FOUND));

            if (!existing.getUserId().equals(userId)) {
                throw new BusinessException("SESSION_ACCESS_DENIED",
                        "Access denied", HttpStatus.FORBIDDEN);
            }
            return existing;
        }

        // Create new session
        return sessionRepository.save(ArchitectSession.builder()
                .userId(userId)
                .title("New Design Session")
                .build());
    }
}
