package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.PluginJar;
import br.com.archbase.plugin.manager.plugin.TestExtension;
import br.com.archbase.plugin.manager.plugin.TestPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

class LegacyExtensionFinderTest {

    @TempDir
    Path pluginsPath;

    @Test
    @EnabledOnOs(WINDOWS)
    void shouldUnlockFileAfterReadingExtensionsFromPlugin() throws Exception {
        PluginJar pluginJar = new PluginJar.Builder(pluginsPath.resolve("test-archbasePlugin.jar"), "test-archbasePlugin")
                .pluginClass(TestPlugin.class.getName())
                .pluginVersion("1.2.3")
                .extension(TestExtension.class.getName())
                .build();

        assertTrue(pluginJar.file().exists());

        ArchbasePluginManager archbasePluginManager = new JarArchbasePluginManager(pluginsPath);
        archbasePluginManager.loadPlugins();

        assertEquals(1, archbasePluginManager.getPlugins().size());

        LegacyExtensionFinder extensionFinder = new LegacyExtensionFinder(archbasePluginManager);
        Map<String, Set<String>> pluginsStorages = extensionFinder.readPluginsStorages();
        assertNotNull(pluginsStorages);

        archbasePluginManager.unloadPlugin(pluginJar.pluginId());
        boolean fileDeleted = pluginJar.file().delete();

        Set<String> pluginStorages = pluginsStorages.get(pluginJar.pluginId());
        assertNotNull(pluginStorages);
        assertEquals(1, pluginStorages.size());
        assertThat(pluginStorages, contains(TestExtension.class.getName()));
        assertTrue(fileDeleted);
        assertFalse(pluginJar.file().exists());
    }

}
