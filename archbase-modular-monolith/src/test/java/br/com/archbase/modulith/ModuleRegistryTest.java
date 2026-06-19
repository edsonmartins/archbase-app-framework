package br.com.archbase.modulith;

import br.com.archbase.modulith.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para ModuleRegistry.
 */
public class ModuleRegistryTest {

    private ModuleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultModuleRegistry();
    }

    @Test
    void shouldRegisterModule() {
        ModuleDescriptor module = ModuleDescriptor.builder()
                .name("test-module")
                .version("1.0.0")
                .enabled(true)
                .build();

        registry.register(module);

        assertTrue(registry.isRegistered("test-module"));
        assertEquals(1, registry.getAllModules().size());
    }

    @Test
    void shouldThrowExceptionWhenRegisteringDuplicateModule() {
        ModuleDescriptor module = ModuleDescriptor.builder()
                .name("test-module")
                .version("1.0.0")
                .enabled(true)
                .build();

        registry.register(module);

        assertThrows(ModuleRegistrationException.class, () -> registry.register(module));
    }

    @Test
    void shouldGetModuleByName() {
        ModuleDescriptor module = ModuleDescriptor.builder()
                .name("test-module")
                .version("1.0.0")
                .enabled(true)
                .build();

        registry.register(module);

        assertTrue(registry.getModule("test-module").isPresent());
        assertFalse(registry.getModule("non-existent").isPresent());
    }

    @Test
    void shouldGetEnabledModulesOnly() {
        registry.register(ModuleDescriptor.builder()
                .name("enabled-module")
                .version("1.0.0")
                .enabled(true)
                .build());

        registry.register(ModuleDescriptor.builder()
                .name("disabled-module")
                .version("1.0.0")
                .enabled(false)
                .build());

        List<ModuleDescriptor> enabled = registry.getEnabledModules();
        assertEquals(1, enabled.size());
        assertEquals("enabled-module", enabled.get(0).getName());
    }

    @Test
    void shouldValidateDependencies() {
        registry.register(ModuleDescriptor.builder()
                .name("module-a")
                .version("1.0.0")
                .enabled(true)
                .dependencies(Set.of("module-b"))
                .build());

        registry.register(ModuleDescriptor.builder()
                .name("module-b")
                .version("1.0.0")
                .enabled(true)
                .build());

        assertDoesNotThrow(() -> registry.validateDependencies());
    }

    @Test
    void shouldThrowExceptionForMissingDependency() {
        registry.register(ModuleDescriptor.builder()
                .name("module-a")
                .version("1.0.0")
                .enabled(true)
                .dependencies(Set.of("non-existent"))
                .build());

        assertThrows(ModuleDependencyException.class, () -> registry.validateDependencies());
    }

    @Test
    void shouldDetectCyclicDependencies() {
        registry.register(ModuleDescriptor.builder()
                .name("module-a")
                .version("1.0.0")
                .enabled(true)
                .dependencies(Set.of("module-b"))
                .build());

        registry.register(ModuleDescriptor.builder()
                .name("module-b")
                .version("1.0.0")
                .enabled(true)
                .dependencies(Set.of("module-a"))
                .build());

        assertThrows(ModuleDependencyException.class, () -> registry.validateDependencies());
    }

    @Test
    void shouldReturnModulesInStartupOrder() {
        registry.register(ModuleDescriptor.builder()
                .name("module-c")
                .version("1.0.0")
                .enabled(true)
                .dependencies(Set.of("module-b"))
                .order(0)
                .build());

        registry.register(ModuleDescriptor.builder()
                .name("module-b")
                .version("1.0.0")
                .enabled(true)
                .dependencies(Set.of("module-a"))
                .order(0)
                .build());

        registry.register(ModuleDescriptor.builder()
                .name("module-a")
                .version("1.0.0")
                .enabled(true)
                .order(0)
                .build());

        List<ModuleDescriptor> order = registry.getStartupOrder();

        assertEquals(3, order.size());
        assertEquals("module-a", order.get(0).getName());
        assertEquals("module-b", order.get(1).getName());
        assertEquals("module-c", order.get(2).getName());
    }

    @Test
    void shouldReturnHealthForModule() {
        ModuleDescriptor module = ModuleDescriptor.builder()
                .name("test-module")
                .version("1.0.0")
                .enabled(true)
                .build();

        module.setState(ModuleState.STARTED);
        registry.register(module);

        ModuleHealth health = registry.getHealth("test-module");
        assertTrue(health.isHealthy());
        assertEquals(ModuleState.STARTED, health.getState());
    }

    @Test
    void shouldUnregisterModule() {
        ModuleDescriptor module = ModuleDescriptor.builder()
                .name("test-module")
                .version("1.0.0")
                .enabled(true)
                .build();

        registry.register(module);
        assertTrue(registry.isRegistered("test-module"));

        boolean removed = registry.unregister("test-module");
        assertTrue(removed);
        assertFalse(registry.isRegistered("test-module"));
    }
}
