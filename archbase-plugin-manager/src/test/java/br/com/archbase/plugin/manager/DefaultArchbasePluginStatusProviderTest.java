package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DefaultpluginStatusProviderTest {

    @TempDir
    Path pluginsPath;

    @Test
    void testIsPluginDisabled() throws IOException {
        createEnabledFile();
        createDisabledFile();

        PluginStatusProvider statusProvider = new DefaultArchbasePluginStatusProvider(pluginsPath);

        assertFalse(statusProvider.isPluginDisabled("plugin-1"));
        assertTrue(statusProvider.isPluginDisabled("plugin-2"));
        assertTrue(statusProvider.isPluginDisabled("plugin-3"));
    }

    @Test
    void testIsPluginDisabledWithEnableEmpty() throws IOException {
        createDisabledFile();

        PluginStatusProvider statusProvider = new DefaultArchbasePluginStatusProvider(pluginsPath);

        assertFalse(statusProvider.isPluginDisabled("plugin-1"));
        assertTrue(statusProvider.isPluginDisabled("plugin-2"));
        assertFalse(statusProvider.isPluginDisabled("plugin-3"));
    }

    @Test
    void testDisablePlugin() throws Exception {
        createEnabledFile();
        createDisabledFile();

        PluginStatusProvider statusProvider = new DefaultArchbasePluginStatusProvider(pluginsPath);
        statusProvider.disablePlugin("plugin-1");

        assertTrue(statusProvider.isPluginDisabled("plugin-1"));
        assertTrue(statusProvider.isPluginDisabled("plugin-2"));
        assertTrue(statusProvider.isPluginDisabled("plugin-3"));
    }

    @Test
    void testDisablePluginWithEnableEmpty() throws Exception {
        // cenário com "disabled.txt"
        createDisabledFile();

        DefaultArchbasePluginStatusProvider statusProvider = new DefaultArchbasePluginStatusProvider(pluginsPath);
        statusProvider.disablePlugin("plugin-1");

        assertTrue(statusProvider.isPluginDisabled("plugin-1"));
        assertTrue(statusProvider.isPluginDisabled("plugin-2"));
        assertFalse(statusProvider.isPluginDisabled("plugin-3"));

        List<String> disabledPlugins = FileUtils.readLines(statusProvider.getDisabledFilePath(), true);
        assertTrue(disabledPlugins.contains("plugin-1"));

        assertTrue(Files.notExists(statusProvider.getEnabledFilePath()));

        // cenário com "enabled.txt"
        Files.delete(statusProvider.getDisabledFilePath());
        assertTrue(Files.notExists(statusProvider.getDisabledFilePath()));

        createEnabledFile();

        statusProvider = new DefaultArchbasePluginStatusProvider(pluginsPath);
        statusProvider.disablePlugin("plugin-1");

        assertTrue(statusProvider.isPluginDisabled("plugin-1"));
        assertFalse(statusProvider.isPluginDisabled("plugin-2"));

        List<String> enabledPlugins = FileUtils.readLines(statusProvider.getEnabledFilePath(), true);
        assertFalse(enabledPlugins.contains("plugin-1"));
    }

    @Test
    void testEnablePlugin() throws Exception {
        // cenário com "enabled.txt"
        createEnabledFile();

        DefaultArchbasePluginStatusProvider statusProvider = new DefaultArchbasePluginStatusProvider(pluginsPath);
        statusProvider.enablePlugin("plugin-2");

        assertFalse(statusProvider.isPluginDisabled("plugin-1"));
        assertFalse(statusProvider.isPluginDisabled("plugin-2"));
        assertTrue(statusProvider.isPluginDisabled("plugin-3"));

        List<String> enabledPlugins = FileUtils.readLines(statusProvider.getEnabledFilePath(), true);
        assertTrue(enabledPlugins.contains("plugin-2"));

        assertTrue(Files.notExists(statusProvider.getDisabledFilePath()));

        // cenário com "disabled.txt"
        Files.delete(statusProvider.getEnabledFilePath());
        assertTrue(Files.notExists(statusProvider.getEnabledFilePath()));

        createDisabledFile();

        statusProvider = new DefaultArchbasePluginStatusProvider(pluginsPath);
        statusProvider.enablePlugin("plugin-2");

        assertFalse(statusProvider.isPluginDisabled("plugin-1"));
        assertFalse(statusProvider.isPluginDisabled("plugin-2"));

        List<String> disabledPlugins = FileUtils.readLines(statusProvider.getDisabledFilePath(), true);
        assertFalse(disabledPlugins.contains("plugin-2"));
    }

    @Test
    void testEnablePluginWithEnableEmpty() {
        PluginStatusProvider statusProvider = new DefaultArchbasePluginStatusProvider(pluginsPath);
        statusProvider.enablePlugin("plugin-2");

        assertFalse(statusProvider.isPluginDisabled("plugin-1"));
        assertFalse(statusProvider.isPluginDisabled("plugin-2"));
        assertFalse(statusProvider.isPluginDisabled("plugin-3"));
    }

    @Test
    void testDisablePluginWithoutDisabledFile() {
        PluginStatusProvider statusProvider = new DefaultArchbasePluginStatusProvider(pluginsPath);

        assertFalse(statusProvider.isPluginDisabled("plugin-1"));

        statusProvider.disablePlugin("plugin-1");
        assertTrue(statusProvider.isPluginDisabled("plugin-1"));
    }

    private void createDisabledFile() throws IOException {
        List<String> disabledPlugins = new ArrayList<>();
        disabledPlugins.add("plugin-2");

        FileUtils.writeLines(disabledPlugins, DefaultArchbasePluginStatusProvider.getDisabledFilePath(pluginsPath));
    }

    private void createEnabledFile() throws IOException {
        List<String> enabledPlugins = new ArrayList<>();
        enabledPlugins.add("plugin-1");
        enabledPlugins.add("plugin-2");

        FileUtils.writeLines(enabledPlugins, DefaultArchbasePluginStatusProvider.getEnabledFilePath(pluginsPath));
    }

}
