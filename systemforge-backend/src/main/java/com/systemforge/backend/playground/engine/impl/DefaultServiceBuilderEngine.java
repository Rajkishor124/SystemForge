package com.systemforge.backend.playground.engine.impl;

import com.systemforge.backend.playground.dto.PlaygroundConfigRequest;
import com.systemforge.backend.playground.dto.PlaygroundGeneratedOutput;
import com.systemforge.backend.playground.engine.PlaceholderResolver;
import com.systemforge.backend.playground.engine.ServiceBuilderEngine;
import com.systemforge.backend.playground.engine.TemplateCompositionContext;
import com.systemforge.backend.playground.feature.FeatureModule;
import com.systemforge.backend.playground.feature.FeatureModuleRegistry;
import com.systemforge.backend.playground.template.ServiceTemplate;
import com.systemforge.backend.playground.template.ServiceTemplateRegistry;
import com.systemforge.backend.playground.template.TemplateStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default implementation of the playground generation engine.
 *
 * <p>Execution flow:
 * <ol>
 *   <li>Resolve base template via {@link ServiceTemplateRegistry}</li>
 *   <li>Initialize {@link TemplateCompositionContext} from template defaults</li>
 *   <li>Resolve applicable feature modules via {@link FeatureModuleRegistry}</li>
 *   <li>Apply modules in topological dependency order (mutate context)</li>
 *   <li>Replace all placeholders via {@link PlaceholderResolver}</li>
 *   <li>Build final {@link PlaygroundGeneratedOutput}</li>
 * </ol>
 *
 * <p>Invariants:
 * <ul>
 *   <li>Stateless — no instance-level mutable state</li>
 *   <li>No if-else for specific features or services</li>
 *   <li>100% polymorphic — all behavior driven by templates and modules</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultServiceBuilderEngine implements ServiceBuilderEngine {

    private final ServiceTemplateRegistry templateRegistry;
    private final FeatureModuleRegistry featureModuleRegistry;
    private final PlaceholderResolver placeholderResolver;

    @Override
    public PlaygroundGeneratedOutput generate(PlaygroundConfigRequest request) {
        log.info("Generating playground output: type={}, variant={}, features={}",
                request.getServiceType(), request.getVariant(), request.getFeatures());

        // ① Resolve base template
        String templateKey = request.getVariant().toTemplateKey();
        ServiceTemplate template = templateRegistry.resolve(templateKey);

        // ② Initialize composition context from template defaults
        TemplateCompositionContext context = new TemplateCompositionContext(
                request.getServiceType(),
                request.getVariant(),
                template.getDefaultPlaceholders(),
                template.getDefaultComponents(),
                template.getRecommendedStack()
        );

        // ③ Resolve feature modules (topologically sorted)
        List<FeatureModule> modules = featureModuleRegistry.resolve(
                request.getFeatures(),
                request.getServiceType(),
                request.getVariant()
        );

        // ④ Apply modules in dependency order (mutate context placeholders)
        for (FeatureModule module : modules) {
            log.debug("Applying feature module: {}", module.getSupportedToggle());
            module.apply(context);
        }

        // ⑤ Resolve all placeholders in template structure
        TemplateStructure resolvedTemplate = placeholderResolver.resolve(
                template.getTemplateStructure(), context);

        // ⑥ Resolve architecture description
        String resolvedArchitecture = placeholderResolver.resolveText(
                template.getArchitectureDescription(), context);

        // ⑦ Build structured output
        PlaygroundGeneratedOutput.CodeSections codeSections =
                PlaygroundGeneratedOutput.CodeSections.builder()
                        .controllerCode(resolvedTemplate.getControllerTemplate())
                        .serviceCode(resolvedTemplate.getServiceTemplate())
                        .configCode(resolvedTemplate.getConfigTemplate())
                        .securityCode(resolvedTemplate.getSecurityTemplate())
                        .build();

        PlaygroundGeneratedOutput.PreviewData previewData =
                PlaygroundGeneratedOutput.PreviewData.builder()
                        .generatedCode(codeSections)
                        .architectureSteps(buildArchitectureSteps(resolvedArchitecture, context))
                        .components(context.getComponents())
                        .techStack(context.getTechStack())
                        .build();

        return PlaygroundGeneratedOutput.builder()
                .serviceType(request.getServiceType())
                .variant(request.getVariant())
                .appliedFeatures(request.getFeatures())
                .preview(previewData)
                .export(null) // Future: full project export
                .build();
    }

    /**
     * Combines the template's base architecture description with
     * feature-contributed architecture steps.
     */
    private List<String> buildArchitectureSteps(
            String resolvedArchitecture,
            TemplateCompositionContext context
    ) {
        List<String> steps = new java.util.ArrayList<>();
        steps.add(resolvedArchitecture);
        steps.addAll(context.getArchitectureSteps());
        return steps;
    }
}
