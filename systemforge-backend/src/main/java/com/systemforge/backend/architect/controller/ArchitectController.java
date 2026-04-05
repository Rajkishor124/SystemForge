package com.systemforge.backend.architect.controller;

import com.systemforge.backend.architect.dto.ArchitectRequest;
import com.systemforge.backend.architect.dto.ArchitectResponse;
import com.systemforge.backend.architect.service.ArchitectService;
import com.systemforge.backend.architect.service.ArchitectService.*;
import com.systemforge.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the AI Architect Agent.
 *
 * <p>Intentionally thin — all logic lives in the service/orchestrator.
 */
@RestController
@RequestMapping("/api/v1/architect")
@RequiredArgsConstructor
@Tag(name = "AI Architect Agent", description = "Design, analyze, and generate backend systems with AI")
public class ArchitectController {

    private final ArchitectService architectService;

    // ─── Chat / Design ─────────────────────────────────────────────────────

    @PostMapping("/chat")
    @Operation(
            summary = "Send a message to the AI Architect",
            description = """
                    Submit a system design request, question, or follow-up.
                    The agent will classify intent and respond with structured reasoning.
                    
                    Include `sessionId` to continue an existing conversation.
                    Omit it to start a new design session.
                    """
    )
    public ResponseEntity<ApiResponse<ArchitectResponse>> chat(
            @Valid @RequestBody ArchitectRequest request
    ) {
        ArchitectResponse response = architectService.process(request);
        return ResponseEntity.ok(ApiResponse.success("Response generated", response));
    }

    // ─── Session Management ────────────────────────────────────────────────

    @GetMapping("/sessions")
    @Operation(summary = "List all architect sessions for the current user")
    public ResponseEntity<ApiResponse<List<ArchitectSessionDto>>> listSessions() {
        List<ArchitectSessionDto> sessions = architectService.listSessions();
        return ResponseEntity.ok(ApiResponse.success("Sessions retrieved", sessions));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get full session with messages and reasoning steps")
    public ResponseEntity<ApiResponse<ArchitectSessionDetailDto>> getSession(
            @PathVariable UUID sessionId
    ) {
        ArchitectSessionDetailDto detail = architectService.getSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session retrieved", detail));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Soft-delete a session")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable UUID sessionId
    ) {
        architectService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session deleted", null));
    }
}
