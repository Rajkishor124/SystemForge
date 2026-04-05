package com.systemforge.backend.playground.template;

import com.systemforge.backend.playground.exception.TemplateNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Auto-discovery registry for all {@link ServiceTemplate} beans.
 *
 * <p>Spring injects all ServiceTemplate implementations at startup.
 * The registry indexes them by {@link ServiceTemplate#getKey()} for O(1) lookup.
 *
 * <p>Adding a new template = create a new Spring bean implementing ServiceTemplate.
 * Zero changes to this registry or the engine.
 */
@Component
@Slf4j
public class ServiceTemplateRegistry {

    private final Map<String, ServiceTemplate> templateMap;

    public ServiceTemplateRegistry(List<ServiceTemplate> templates) {
        this.templateMap = templates.stream()
                .collect(Collectors.toMap(ServiceTemplate::getKey, Function.identity()));

        log.info("Playground template registry initialized with {} templates: {}",
                templateMap.size(), templateMap.keySet());
    }

    /**
     * Resolves a template by its key.
     *
     * @throws TemplateNotFoundException if no template is registered for the key
     */
    public ServiceTemplate resolve(String key) {
        ServiceTemplate template = templateMap.get(key);
        if (template == null) {
            throw new TemplateNotFoundException(key);
        }
        return template;
    }

    /**
     * Checks if a template exists for the given key.
     */
    public boolean hasTemplate(String key) {
        return templateMap.containsKey(key);
    }
}
