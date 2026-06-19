package br.com.archbase.modulith.spring;

import br.com.archbase.modulith.core.ModuleHealth;
import br.com.archbase.modulith.core.ModuleRegistry;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health Indicator para módulos do Modular Monolith.
 * <p>
 * Expõe informações de saúde de todos os módulos através do
 * endpoint /actuator/health.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@Component
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnProperty(prefix = "archbase.modulith", name = "health-checks-enabled", havingValue = "true", matchIfMissing = true)
public class ModuleHealthIndicator implements HealthIndicator {

    private final ModuleRegistry moduleRegistry;

    public ModuleHealthIndicator(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    @Override
    public Health health() {
        List<ModuleHealth> allHealth = moduleRegistry.getAllHealth();

        if (allHealth.isEmpty()) {
            return Health.unknown()
                    .withDetail("message", "No modules registered")
                    .build();
        }

        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;
        int healthyCount = 0;
        int unhealthyCount = 0;

        for (ModuleHealth moduleHealth : allHealth) {
            Map<String, Object> moduleDetails = new HashMap<>();
            moduleDetails.put("status", moduleHealth.isHealthy() ? "UP" : "DOWN");
            moduleDetails.put("state", moduleHealth.getState().name());
            moduleDetails.put("message", moduleHealth.getMessage());
            moduleDetails.put("lastChecked", moduleHealth.getLastChecked().toString());

            if (moduleHealth.getDetails() != null) {
                moduleDetails.put("details", moduleHealth.getDetails());
            }

            details.put(moduleHealth.getModuleName(), moduleDetails);

            if (moduleHealth.isHealthy()) {
                healthyCount++;
            } else {
                unhealthyCount++;
                allHealthy = false;
            }
        }

        details.put("summary", Map.of(
                "total", allHealth.size(),
                "healthy", healthyCount,
                "unhealthy", unhealthyCount
        ));

        Health.Builder builder = allHealthy ? Health.up() : Health.down();
        return builder.withDetails(details).build();
    }
}
