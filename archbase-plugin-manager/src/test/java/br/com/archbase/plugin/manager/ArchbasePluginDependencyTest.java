package br.com.archbase.plugin.manager;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class ArchbasePluginDependencyTest {

    /**
     * Teste do método getPluginId, da classe PluginDependency.
     */
    @Test
    @SuppressWarnings("java:S5961")
    void testPluginDependecy() {
        PluginDependency instance = new PluginDependency("test");
        PluginDependency instance2 = new PluginDependency("test");
        assertEquals(instance, instance2);
        assertEquals("test", instance.getPluginId());
        assertEquals("*", instance.getPluginVersionSupport());
        assertFalse(instance.isOptional());

        instance = new PluginDependency("test@");
        instance2 = new PluginDependency("test@");
        assertEquals(instance, instance2);
        assertEquals("test", instance.getPluginId());
        assertEquals("*", instance.getPluginVersionSupport());
        assertFalse(instance.isOptional());

        instance = new PluginDependency("test?");
        instance2 = new PluginDependency("test?");
        assertEquals(instance, instance2);
        assertEquals("test", instance.getPluginId());
        assertEquals("*", instance.getPluginVersionSupport());
        assertTrue(instance.isOptional());

        instance = new PluginDependency("test?@");
        instance2 = new PluginDependency("test?@");
        assertEquals(instance, instance2);
        assertEquals("test", instance.getPluginId());
        assertEquals("*", instance.getPluginVersionSupport());
        assertTrue(instance.isOptional());

        instance = new PluginDependency("test@1.0");
        instance2 = new PluginDependency("test@1.0");
        assertEquals(instance, instance2);
        assertEquals("test", instance.getPluginId());
        assertEquals("1.0", instance.getPluginVersionSupport());
        assertFalse(instance.isOptional());
        assertEquals("PluginDependency [pluginId=test, pluginVersionSupport=1.0, optional=false]", instance.toString());

        instance = new PluginDependency("test?@1.0");
        instance2 = new PluginDependency("test?@1.0");
        assertEquals(instance, instance2);
        assertEquals("test", instance.getPluginId());
        assertEquals("1.0", instance.getPluginVersionSupport());
        assertTrue(instance.isOptional());
        assertEquals("PluginDependency [pluginId=test, pluginVersionSupport=1.0, optional=true]", instance.toString());
    }

}
