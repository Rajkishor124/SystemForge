package com.systemforge.backend.template.controller;

import com.systemforge.backend.common.dto.ApiResponse;
import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.template.dto.TemplateDto;
import com.systemforge.backend.template.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Predefined architecture template library")
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    @Operation(summary = "List templates", description = "Returns templates, optionally filtered by appType and scale")
    public ResponseEntity<ApiResponse<List<TemplateDto>>> getTemplates(
            @RequestParam(required = false) AppType appType,
            @RequestParam(required = false) AppScale appScale) {

        List<TemplateDto> templates;
        if (appType != null && appScale != null) {
            templates = templateService.getByAppTypeAndScale(appType, appScale);
        } else if (appType != null) {
            templates = templateService.getByAppType(appType);
        } else {
            templates = templateService.getAllTemplates();
        }

        return ResponseEntity.ok(ApiResponse.success("Templates retrieved", templates));
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "Get template by ID")
    public ResponseEntity<ApiResponse<TemplateDto>> getTemplateById(@PathVariable UUID templateId) {
        return ResponseEntity.ok(ApiResponse.success("Template retrieved",
                templateService.getById(templateId)));
    }
}