package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.PluginJar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

class ManifestArchbasePluginDescriptorFinderTest {

    @TempDir
    Path pluginsPath;
    private VersionManager versionManager;

    @BeforeEach
    void setUp() throws IOException {
        Path pluginPath = Files.createDirectories(pluginsPath.resolve("test-archbasePlugin-1"));
        storeManifestToPath(getPlugin1Manifest(), pluginPath);

        pluginPath = Files.createDirectories(pluginsPath.resolve("test-archbasePlugin-2"));
        storeManifestToPath(getPlugin2Manifest(), pluginPath);

        // empty archbasePlugin
        Files.createDirectories(pluginsPath.resolve("test-archbasePlugin-3"));

        // nenhuma classe de archbasePlugin
        pluginPath = Files.createDirectories(pluginsPath.resolve("test-archbasePlugin-4"));
        storeManifestToPath(getPlugin4Manifest(), pluginPath);

        // no archbasePlugin version
        pluginPath = Files.createDirectories(pluginsPath.resolve("test-archbasePlugin-5"));
        storeManifestToPath(getPlugin5Manifest(), pluginPath);

        // sem archbasePlugin id
        pluginPath = Files.createDirectories(pluginsPath.resolve("test-archbasePlugin-6"));
        storeManifestToPath(getPlugin6Manifest(), pluginPath);

        versionManager = new DefaultArchbaseVersionManager();
    }

    /**
     * Teste para o método {@link ManifestPluginDescriptorFinder#find(Path)}.
     */
    @Test
    void testFind() throws Exception {
        PluginDescriptorFinder descriptorFinder = new ManifestPluginDescriptorFinder();

        PluginDescriptor plugin1 = descriptorFinder.find(pluginsPath.resolve("test-archbasePlugin-1"));
        PluginDescriptor plugin2 = descriptorFinder.find(pluginsPath.resolve("test-archbasePlugin-2"));

        assertEquals("test-archbasePlugin-1", plugin1.getPluginId());
        assertEquals("Test ArchbasePlugin 1", plugin1.getPluginDescription());
        assertEquals("TestPlugin", plugin1.getPluginClass());
        assertEquals("0.0.1", plugin1.getVersion());
        assertEquals("Archbase Inc", plugin1.getProvider());
        assertEquals(2, plugin1.getDependencies().size());
        assertEquals("test-archbasePlugin-2", plugin1.getDependencies().get(0).getPluginId());
        assertEquals("test-archbasePlugin-3", plugin1.getDependencies().get(1).getPluginId());
        assertEquals("~1.0", plugin1.getDependencies().get(1).getPluginVersionSupport());
        assertEquals("Apache-2.0", plugin1.getLicense());
        assertTrue(versionManager.checkVersionConstraint("1.0.0", plugin1.getRequires()));

        assertEquals("test-archbasePlugin-2", plugin2.getPluginId());
        assertEquals("", plugin2.getPluginDescription());
        assertEquals("TestPlugin", plugin2.getPluginClass());
        assertEquals("0.0.1", plugin2.getVersion());
        assertEquals("Archbase Inc", plugin2.getProvider());
        assertEquals(0, plugin2.getDependencies().size());
        assertTrue(versionManager.checkVersionConstraint("1.0.0", plugin2.getRequires()));
    }

    /**
     * Teste para o método {@link ManifestPluginDescriptorFinder#find(Path)}.
     */
    @Test
    @SuppressWarnings("java:S5778")
    void testFindNotFound() {
        PluginDescriptorFinder descriptorFinder = new ManifestPluginDescriptorFinder();
        assertThrows(PluginRuntimeException.class, () -> descriptorFinder.find(pluginsPath.resolve("test-archbasePlugin-3")));
    }

    private Manifest getPlugin1Manifest() {
        Map<String, String> map = new LinkedHashMap<>(8);
        map.put(ManifestPluginDescriptorFinder.PLUGIN_ID, "test-archbasePlugin-1");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_CLASS, "TestPlugin");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_VERSION, "0.0.1");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_DESCRIPTION, "Test ArchbasePlugin 1");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_DEPENDENCIES, "test-archbasePlugin-2,test-archbasePlugin-3@~1.0");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_REQUIRES, "*");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_LICENSE, "Apache-2.0");

        return PluginJar.createManifest(map);
    }

    private Manifest getPlugin2Manifest() {
        Map<String, String> map = new LinkedHashMap<>(5);
        map.put(ManifestPluginDescriptorFinder.PLUGIN_ID, "test-archbasePlugin-2");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_CLASS, "TestPlugin");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_VERSION, "0.0.1");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_DEPENDENCIES, "");

        return PluginJar.createManifest(map);
    }

    private Manifest getPlugin4Manifest() {
        Map<String, String> map = new LinkedHashMap<>(3);
        map.put(ManifestPluginDescriptorFinder.PLUGIN_ID, "test-archbasePlugin-1");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_VERSION, "0.0.1");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");

        return PluginJar.createManifest(map);
    }

    private Manifest getPlugin5Manifest() {
        Map<String, String> map = new LinkedHashMap<>(3);
        map.put(ManifestPluginDescriptorFinder.PLUGIN_ID, "test-archbasePlugin-2");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_CLASS, "TestPlugin");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");

        return PluginJar.createManifest(map);
    }

    private Manifest getPlugin6Manifest() {
        Map<String, String> map = new LinkedHashMap<>(2);
        map.put(ManifestPluginDescriptorFinder.PLUGIN_CLASS, "TestPlugin");
        map.put(ManifestPluginDescriptorFinder.PLUGIN_PROVIDER, "Archbase Inc");

        return PluginJar.createManifest(map);
    }

    private void storeManifestToPath(Manifest manifest, Path pluginPath) throws IOException {
        Path path = Files.createDirectory(pluginPath.resolve("META-INF"));
        try (OutputStream output = new FileOutputStream(path.resolve("MANIFEST.MF").toFile())) {
            manifest.write(output);
        }
    }

}
