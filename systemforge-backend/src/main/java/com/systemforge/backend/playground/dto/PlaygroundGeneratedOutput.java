package com.systemforge.backend.playground.dto;

import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Output DTO for a generated playground architecture.
 *
 * <p>Split into {@link PreviewData} (immediate display) and
 * {@link ExportData} (future full-project export).
 * MVP populates PreviewData only.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaygroundGeneratedOutput {

    private String id;
    private java.time.LocalDateTime createdAt;

    private ServiceType serviceType;
    private ServiceVariant variant;
    private List<FeatureToggle> appliedFeatures;

    private PreviewData preview;
    private ExportData export;

    // ─── Preview: Immediate display in the UI ──────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PreviewData {

        /** Syntax-highlighted code sections keyed by file name */
        private CodeSections generatedCode;

        /** Ordered architecture description steps */
        private List<String> architectureSteps;

        /** Component modules used in this architecture */
        private List<String> components;

        /** Recommended tech stack */
        private List<String> techStack;
    }

    // ─── Code Sections: Structured, not a single blob ──────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CodeSections {
        private String controllerCode;
        private String serviceCode;
        private String configCode;
        private String securityCode;
    }

    // ─── Export: Future-ready for full project generation ───────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportData {
        /** Placeholder for full project ZIP structure in future */
        private String projectStructureJson;
    }
}
