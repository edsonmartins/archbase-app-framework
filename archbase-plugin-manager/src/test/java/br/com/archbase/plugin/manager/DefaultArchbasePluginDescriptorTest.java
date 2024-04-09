package br.com.archbase.plugin.manager;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultArchbasePluginDescriptorTest {

    @Test
    void addDependency() {
        // Dado um descritor com dependências vazias
        DefaultArchbasePluginDescriptor descriptor = new DefaultArchbasePluginDescriptor();
        descriptor.setDependencies("");
        PluginDependency newDependency = new PluginDependency("test");

        // When I add a dependency
        descriptor.addDependency(newDependency);

        // Então a dependência é adicionada
        List<PluginDependency> expected = new ArrayList<>();
        expected.add(newDependency);
        assertEquals(expected, descriptor.getDependencies());
    }
}
