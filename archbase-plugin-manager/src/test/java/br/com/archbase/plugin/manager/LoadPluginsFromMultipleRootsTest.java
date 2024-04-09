package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.PluginZip;
import br.com.archbase.plugin.manager.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadPluginsFromMultipleRootsTest {

    Path pluginsPath1;
    Path pluginsPath2;
    private DefaultArchbasePluginManager pluginManager;

    @BeforeEach
    void setUp() throws IOException {
        pluginsPath1 = Files.createTempDirectory("junit-archbase-");
        pluginsPath2 = Files.createTempDirectory("junit-archbase-");
        pluginManager = new DefaultArchbasePluginManager(pluginsPath1, pluginsPath2);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.delete(pluginsPath1);
        FileUtils.delete(pluginsPath2);
    }

    @Test
    void load() throws Exception {
        PluginZip pluginZip1 = new PluginZip.Builder(pluginsPath1.resolve("my-plugin-1.2.3.zip"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();

        PluginZip pluginZip2 = new PluginZip.Builder(pluginsPath2.resolve("my-other-plugin-4.5.6.zip"), "myOtherPlugin")
                .pluginVersion("4.5.6")
                .build();

        assertTrue(Files.exists(pluginZip1.path()));
        assertEquals(0, pluginManager.getPlugins().size());

        pluginManager.loadPlugins();

        assertTrue(Files.exists(pluginZip1.path()));
        assertTrue(Files.exists(pluginZip1.unzippedPath()));
        assertTrue(Files.exists(pluginZip2.path()));
        assertTrue(Files.exists(pluginZip2.unzippedPath()));
        assertEquals(2, pluginManager.getPlugins().size());
        assertEquals(pluginZip1.pluginId(), pluginManager.idForPath(pluginZip1.unzippedPath()));
        assertEquals(pluginZip2.pluginId(), pluginManager.idForPath(pluginZip2.unzippedPath()));
    }

}
