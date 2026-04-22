package com.systemforge.backend.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the specialized roles in the Multi-Agent Backend Architecture (MABA).
 */
@Getter
@RequiredArgsConstructor
public enum AgentRole {
    ORCHESTRATOR("orchestrator_prime", "ORCHESTRATOR PRIME", "Ω"),
    RAG_ENGINE("rag_engine", "RAG KNOWLEDGE ENGINE", "◈"),
    REQUIREMENTS_ANALYST("requirements_analyst", "REQUIREMENTS ANALYST", "⧉"),
    SYSTEM_ARCHITECT("system_architect", "SYSTEM ARCHITECT", "◬"),
    DB_DESIGNER("db_designer", "DATABASE ARCHITECT", "◉"),
    API_DESIGNER("api_designer", "API DESIGN ENGINEER", "⟳"),
    SCALABILITY_ENGINEER("scalability_engineer", "SCALABILITY ENGINEER", "⟁"),
    SECURITY_ENGINEER("security_engineer", "SECURITY ENGINEER", "⬡"),
    IMPLEMENTATION_PLANNER("implementation_planner", "IMPLEMENTATION PLANNER", "▣"),
    FINAL_SYNTHESIZER("final_synthesizer", "FINAL SYNTHESIZER", "Σ");

    private final String id;
    private final String roleName;
    private final String badge;
}
