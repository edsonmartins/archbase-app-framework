package br.com.archbase.plugin.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DependencyResolverTest {

    private DependencyResolver resolver;

    @BeforeEach
    public void init() {
        VersionManager versionManager = new DefaultArchbaseVersionManager();
        resolver = new DependencyResolver(versionManager);
    }

    @Test
    void sortedPlugins() {
        // create incomplete archbasePlugin descriptor (ignore some attributes)
        PluginDescriptor pd1 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p1")
                .setDependencies("p2");

        PluginDescriptor pd2 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p2")
                .setPluginVersion("0.0.0"); // needed in "checkDependencyVersion" method

        List<PluginDescriptor> plugins = new ArrayList<>();
        plugins.add(pd1);
        plugins.add(pd2);

        DependencyResolver.Result result = resolver.resolve(plugins);

        assertTrue(result.getNotFoundDependencies().isEmpty());
        assertEquals(Arrays.asList("p2", "p1"), result.getSortedPlugins());
    }

    @Test
    void notFoundDependencies() {
        PluginDescriptor pd1 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p1")
                .setDependencies("p2, p3");

        List<PluginDescriptor> plugins = new ArrayList<>();
        plugins.add(pd1);

        DependencyResolver.Result result = resolver.resolve(plugins);

        assertFalse(result.getNotFoundDependencies().isEmpty());
        assertEquals(Arrays.asList("p2", "p3"), result.getNotFoundDependencies());
    }

    @Test
    void cyclicDependencies() {
        PluginDescriptor pd1 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p1")
                .setPluginVersion("0.0.0")
                .setDependencies("p2");

        PluginDescriptor pd2 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p2")
                .setPluginVersion("0.0.0")
                .setDependencies("p3");

        PluginDescriptor pd3 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p3")
                .setPluginVersion("0.0.0")
                .setDependencies("p1");

        List<PluginDescriptor> plugins = new ArrayList<>();
        plugins.add(pd1);
        plugins.add(pd2);
        plugins.add(pd3);

        DependencyResolver.Result result = resolver.resolve(plugins);

        assertFalse(result.hasCyclicDependency());
    }

    @Test
    void wrongDependencyVersion() {
        PluginDescriptor pd1 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p1")
                .setDependencies("p2@>=1.5.0 & <1.6.0"); // versão do intervalo

        PluginDescriptor pd2 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p2")
                .setPluginVersion("1.4.0");

        List<PluginDescriptor> plugins = new ArrayList<>();
        plugins.add(pd1);
        plugins.add(pd2);

        DependencyResolver.Result result = resolver.resolve(plugins);

        assertFalse(result.getWrongVersionDependencies().isEmpty());
    }

    @Test
    void goodDependencyVersion() {
        PluginDescriptor pd1 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p1")
                .setDependencies("p2@2.0.0");

        PluginDescriptor pd2 = new DefaultArchbasePluginDescriptor()
                .setPluginId("p2")
                .setPluginVersion("2.0.0");

        List<PluginDescriptor> plugins = new ArrayList<>();
        plugins.add(pd1);
        plugins.add(pd2);

        DependencyResolver.Result result = resolver.resolve(plugins);

        assertTrue(result.getWrongVersionDependencies().isEmpty());
    }

}