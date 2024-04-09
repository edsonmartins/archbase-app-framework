package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.PluginZip;
import br.com.archbase.plugin.manager.plugin.TestPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesArchbasePluginDescriptorFinderTest {

    @TempDir
    Path pluginsPath;
    private VersionManager versionManager;

    @BeforeEach
    void setUp() throws IOException {
        Path pluginPath = Files.createDirectory(pluginsPath.resolve("test-archbasePlugin-1"));
        storePropertiesToPath(getPlugin1Properties(), pluginPath);

        pluginPath = Files.createDirectory(pluginsPath.resolve("test-archbasePlugin-2"));
        storePropertiesToPath(getPlugin2Properties(), pluginPath);

        // archbasePlugin vazio
        Files.createDirectories(pluginsPath.resolve("test-archbasePlugin-3"));

        // sem classe de archbasePlugin
        pluginPath = Files.createDirectory(pluginsPath.resolve("test-archbasePlugin-4"));
        storePropertiesToPath(getPlugin4Properties(), pluginPath);

        // nenhuma versão do archbasePlugin
        pluginPath = Files.createDirectory(pluginsPath.resolve("test-archbasePlugin-5"));
        storePropertiesToPath(getPlugin5Properties(), pluginPath);

        // sem id de archbasePlugin
        pluginPath = Files.createDirectory(pluginsPath.resolve("test-archbasePlugin-6"));
        storePropertiesToPath(getPlugin6Properties(), pluginPath);

        versionManager = new DefaultArchbaseVersionManager();
    }

    @Test
    void testFind() throws Exception {
        PluginDescriptorFinder descriptorFinder = new PropertiesPluginDescriptorFinder();

        PluginDescriptor plugin1 = descriptorFinder.find(pluginsPath.resolve("test-archbasePlugin-1"));
        PluginDescriptor plugin2 = descriptorFinder.find(pluginsPath.resolve("test-archbasePlugin-2"));

        assertEquals("test-archbasePlugin-1", plugin1.getPluginId());
        assertEquals("Test ArchbasePlugin 1", plugin1.getPluginDescription());
        assertEquals(TestPlugin.class.getName(), plugin1.getPluginClass());
        assertEquals("0.0.1", plugin1.getVersion());
        assertEquals("Archbase Inc", plugin1.getProvider());
        assertEquals(2, plugin1.getDependencies().size());
        assertEquals("test-archbasePlugin-2", plugin1.getDependencies().get(0).getPluginId());
        assertEquals("test-archbasePlugin-3", plugin1.getDependencies().get(1).getPluginId());
        assertEquals("~1.0", plugin1.getDependencies().get(1).getPluginVersionSupport());
        assertEquals("Apache-2.0", plugin1.getLicense());
        assertEquals(">=1", plugin1.getRequires());
        assertTrue(versionManager.checkVersionConstraint("1.0.0", plugin1.getRequires()));
        assertFalse(versionManager.checkVersionConstraint("0.1.0", plugin1.getRequires()));

        assertEquals("test-archbasePlugin-2", plugin2.getPluginId());
        assertEquals("", plugin2.getPluginDescription());
        assertEquals(TestPlugin.class.getName(), plugin2.getPluginClass());
        assertEquals("0.0.1", plugin2.getVersion());
        assertEquals("Archbase Inc", plugin2.getProvider());
        assertEquals(0, plugin2.getDependencies().size());
        assertEquals("*", plugin2.getRequires()); // Default is *
        assertTrue(versionManager.checkVersionConstraint("1.0.0", plugin2.getRequires()));
    }

    @Test
    @SuppressWarnings("java:S5778")
    void testNotFound() {
        PluginDescriptorFinder descriptorFinder = new PropertiesPluginDescriptorFinder();
        assertThrows(PluginRuntimeException.class, () -> descriptorFinder.find(pluginsPath.resolve("test-archbasePlugin-3")));
    }

    private Properties getPlugin1Properties() {
        Map<String, String> map = new LinkedHashMap<>(8);
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_ID, "test-archbasePlugin-1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_CLASS, TestPlugin.class.getName());
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_VERSION, "0.0.1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_DESCRIPTION, "Test ArchbasePlugin 1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_DEPENDENCIES, "test-archbasePlugin-2,test-archbasePlugin-3@~1.0");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_REQUIRES, ">=1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_LICENSE, "Apache-2.0");

        return PluginZip.createProperties(map);
    }

    private Properties getPlugin2Properties() {
        Map<String, String> map = new LinkedHashMap<>(5);
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_ID, "test-archbasePlugin-2");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_CLASS, TestPlugin.class.getName());
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_VERSION, "0.0.1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_DEPENDENCIES, "");

        return PluginZip.createProperties(map);
    }

    private Properties getPlugin4Properties() {
        Map<String, String> map = new LinkedHashMap<>(5);
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_ID, "test-archbasePlugin-2");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_VERSION, "0.0.1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_DEPENDENCIES, "");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_REQUIRES, "*");

        return PluginZip.createProperties(map);
    }

    private Properties getPlugin5Properties() {
        Map<String, String> map = new LinkedHashMap<>(5);
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_ID, "test-archbasePlugin-2");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_CLASS, TestPlugin.class.getName());
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_DEPENDENCIES, "");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_REQUIRES, "*");

        return PluginZip.createProperties(map);
    }

    private Properties getPlugin6Properties() {
        Map<String, String> map = new LinkedHashMap<>(5);
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_CLASS, TestPlugin.class.getName());
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_VERSION, "0.0.1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_DEPENDENCIES, "");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_REQUIRES, "*");

        return PluginZip.createProperties(map);
    }

    private void storePropertiesToPath(Properties properties, Path pluginPath) throws IOException {
        Path path = pluginPath.resolve(PropertiesPluginDescriptorFinder.DEFAULT_PROPERTIES_FILE_NAME);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
            properties.store(writer, "");
        }
    }

}
