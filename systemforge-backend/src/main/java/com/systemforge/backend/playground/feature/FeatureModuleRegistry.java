package com.systemforge.backend.playground.feature;

import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;
import com.systemforge.backend.playground.exception.IncompatibleFeatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry that resolves and topologically sorts feature modules.
 *
 * <p>Spring auto-injects all {@link FeatureModule} beans.
 * This registry indexes them by toggle and provides a sorted execution order
 * based on declared dependencies (topological sort with cycle detection).
 */
@Component
@Slf4j
public class FeatureModuleRegistry {

    private final Map<FeatureToggle, FeatureModule> moduleMap;

    public FeatureModuleRegistry(List<FeatureModule> modules) {
        this.moduleMap = modules.stream()
                .collect(Collectors.toMap(FeatureModule::getSupportedToggle, Function.identity()));

        log.info("Feature module registry initialized with {} modules: {}",
                moduleMap.size(), moduleMap.keySet());
    }

    /**
     * Resolves and returns feature modules in topologically sorted order.
     *
     * <p>Steps:
     * <ol>
     *   <li>Look up each requested toggle in the registry</li>
     *   <li>Validate compatibility with the service type + variant</li>
     *   <li>Topologically sort by declared dependencies</li>
     * </ol>
     *
     * @throws IncompatibleFeatureException if a feature is incompatible or has a missing dependency
     */
    public List<FeatureModule> resolve(
            List<FeatureToggle> requestedToggles,
            ServiceType serviceType,
            ServiceVariant variant
    ) {
        if (requestedToggles == null || requestedToggles.isEmpty()) {
            return List.of();
        }

        Set<FeatureToggle> requestedSet = new LinkedHashSet<>(requestedToggles);
        List<FeatureModule> resolved = new ArrayList<>();

        for (FeatureToggle toggle : requestedToggles) {
            FeatureModule module = moduleMap.get(toggle);
            if (module == null) {
                log.warn("No module registered for toggle: {}", toggle);
                continue;
            }

            // Compatibility check
            if (!module.isCompatibleWith(serviceType, variant)) {
                throw new IncompatibleFeatureException(toggle, serviceType);
            }

            // Dependency check
            for (FeatureToggle dep : module.getDependencies()) {
                if (!requestedSet.contains(dep)) {
                    throw new IncompatibleFeatureException(toggle, dep);
                }
            }

            resolved.add(module);
        }

        return topologicalSort(resolved);
    }

    /**
     * Kahn's algorithm for topological sort with cycle detection.
     */
    private List<FeatureModule> topologicalSort(List<FeatureModule> modules) {
        if (modules.size() <= 1) return modules;

        Map<FeatureToggle, FeatureModule> moduleIndex = modules.stream()
                .collect(Collectors.toMap(FeatureModule::getSupportedToggle, Function.identity()));

        Set<FeatureToggle> activeToggles = moduleIndex.keySet();

        // Build in-degree map (only for dependencies within the active set)
        Map<FeatureToggle, Integer> inDegree = new LinkedHashMap<>();
        Map<FeatureToggle, List<FeatureToggle>> adjacency = new LinkedHashMap<>();

        for (FeatureToggle toggle : activeToggles) {
            inDegree.put(toggle, 0);
            adjacency.put(toggle, new ArrayList<>());
        }

        for (FeatureModule module : modules) {
            for (FeatureToggle dep : module.getDependencies()) {
                if (activeToggles.contains(dep)) {
                    adjacency.get(dep).add(module.getSupportedToggle());
                    inDegree.merge(module.getSupportedToggle(), 1, Integer::sum);
                }
            }
        }

        // Kahn's BFS
        Queue<FeatureToggle> queue = new LinkedList<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<FeatureModule> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            FeatureToggle current = queue.poll();
            sorted.add(moduleIndex.get(current));

            for (FeatureToggle dependent : adjacency.getOrDefault(current, List.of())) {
                int newDegree = inDegree.get(dependent) - 1;
                inDegree.put(dependent, newDegree);
                if (newDegree == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (sorted.size() != modules.size()) {
            log.error("Circular dependency detected in feature modules! Falling back to unordered.");
            return modules;
        }

        return sorted;
    }
}
