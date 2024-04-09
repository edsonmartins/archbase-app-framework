package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.PluginJar;
import br.com.archbase.plugin.manager.plugin.TestExtension;
import br.com.archbase.plugin.manager.plugin.TestExtensionPoint;
import br.com.archbase.plugin.manager.plugin.TestPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JarArchbasePluginManagerTest {

    @TempDir
    Path pluginsPath;
    private PluginJar pluginJar;
    private JarArchbasePluginManager pluginManager;

    @BeforeEach
    void setUp() throws IOException {
        pluginJar = new PluginJar.Builder(pluginsPath.resolve("test-archbasePlugin.jar"), "test-archbasePlugin")
                .pluginClass(TestPlugin.class.getName())
                .pluginVersion("1.2.3")
                .extension(TestExtension.class.getName())
                .build();

        pluginManager = new JarArchbasePluginManager(pluginsPath);
    }

    @AfterEach
    void tearDown() {
        pluginManager.unloadPlugins();

        pluginJar = null;
        pluginManager = null;
    }

    @Test
    void getExtensions() {
        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        List<TestExtensionPoint> extensions = pluginManager.getExtensions(TestExtensionPoint.class);
        Integer size = extensions.size();
        assertEquals(Integer.valueOf(2), size);

        String something = extensions.get(0).saySomething();
        assertEquals(new TestExtension().saySomething(), something);
    }

    @Test
    void unloadPlugin() throws Exception {
        pluginManager.loadPlugins();

        assertEquals(1, pluginManager.getPlugins().size());

        boolean unloaded = pluginManager.unloadPlugin(pluginJar.pluginId());
        assertTrue(unloaded);

        assertTrue(pluginJar.file().exists());
    }

    @Test
    void deletePlugin() throws Exception {
        pluginManager.loadPlugins();

        assertEquals(1, pluginManager.getPlugins().size());

        boolean deleted = pluginManager.deletePlugin(pluginJar.pluginId());
        assertTrue(deleted);

        assertFalse(pluginJar.file().exists());
    }

}
