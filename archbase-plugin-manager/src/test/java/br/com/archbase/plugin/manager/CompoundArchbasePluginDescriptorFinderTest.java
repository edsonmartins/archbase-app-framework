package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.PluginJar;
import br.com.archbase.plugin.manager.plugin.PluginZip;
import br.com.archbase.plugin.manager.plugin.TestPlugin;
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


class CompoundArchbasePluginDescriptorFinderTest {

    @TempDir
    Path pluginsPath;

    @Test
    void add() {
        CompoundPluginDescriptorFinder descriptorFinder = new CompoundPluginDescriptorFinder();
        assertEquals(0, descriptorFinder.size());

        descriptorFinder.add(new PropertiesPluginDescriptorFinder());
        assertEquals(1, descriptorFinder.size());
    }

    @Test
    void find() throws Exception {
        Path pluginPath = Files.createDirectories(pluginsPath.resolve("test-plugin-1"));
        storePropertiesToPath(getPlugin1Properties(), pluginPath);

        PluginDescriptorFinder descriptorFinder = new CompoundPluginDescriptorFinder()
                .add(new PropertiesPluginDescriptorFinder());

        PluginDescriptor pluginDescriptor = descriptorFinder.find(pluginPath);
        assertNotNull(pluginDescriptor);
        assertEquals("test-plugin-1", pluginDescriptor.getPluginId());
        assertEquals("0.0.1", pluginDescriptor.getVersion());
    }

    @Test
    void findInJar() throws Exception {
        PluginDescriptorFinder descriptorFinder = new CompoundPluginDescriptorFinder()
                .add(new ManifestPluginDescriptorFinder());

        PluginJar pluginJar = new PluginJar.Builder(pluginsPath.resolve("my-plugin-1.2.3.jar"), "myPlugin")
                .pluginClass(TestPlugin.class.getName())
                .pluginVersion("1.2.3")
                .build();

        PluginDescriptor pluginDescriptor = descriptorFinder.find(pluginJar.path());
        assertNotNull(pluginDescriptor);
        assertEquals("myPlugin", pluginJar.pluginId());
        assertEquals(TestPlugin.class.getName(), pluginJar.pluginClass());
        assertEquals("1.2.3", pluginJar.pluginVersion());
    }

    @Test
    @SuppressWarnings("java:S5778")
    void testNotFound() {
        PluginDescriptorFinder descriptorFinder = new CompoundPluginDescriptorFinder();
        assertThrows(PluginRuntimeException.class, () -> {
            descriptorFinder.find(pluginsPath.resolve("test-plugin-3"));
        });
    }

    @Test
    void testSpaceCharacterInFileName() throws Exception {
        PluginDescriptorFinder descriptorFinder = new CompoundPluginDescriptorFinder()
                .add(new ManifestPluginDescriptorFinder());

        PluginJar pluginJar = new PluginJar.Builder(pluginsPath.resolve("my plugin-1.2.3.jar"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();

        PluginDescriptor pluginDescriptor = descriptorFinder.find(pluginJar.path());
        assertNotNull(pluginDescriptor);
    }

    private Properties getPlugin1Properties() {
        Map<String, String> map = new LinkedHashMap<>(7);
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_ID, "test-plugin-1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_CLASS, TestPlugin.class.getName());
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_VERSION, "0.0.1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inch");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_DEPENDENCIES, "test-plugin-2,test-plugin-3@~1.0");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_REQUIRES, ">=1");
        map.put(PropertiesPluginDescriptorFinder.PLUGIN_LICENSE, "Apache-2.0");

        return PluginZip.createProperties(map);
    }

    private void storePropertiesToPath(Properties properties, Path pluginPath) throws IOException {
        Path path = pluginPath.resolve(PropertiesPluginDescriptorFinder.DEFAULT_PROPERTIES_FILE_NAME);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
            properties.store(writer, "");
        }
    }

}
