package br.com.archbase.plugin.manager;


import br.com.archbase.plugin.manager.plugin.PluginZip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LoadPluginsTest {

    @TempDir
    Path pluginsPath;
    private DefaultArchbasePluginManager pluginManager;

    @BeforeEach
    public void setUp() {
        pluginManager = new DefaultArchbasePluginManager(pluginsPath);
    }

    @Test
    void load() throws Exception {
        PluginZip pluginZip = new PluginZip.Builder(pluginsPath.resolve("my-plugin-1.2.3.zip"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();

        assertTrue(Files.exists(pluginZip.path()));
        assertEquals(0, pluginManager.getPlugins().size());
        pluginManager.loadPlugins();

        assertTrue(Files.exists(pluginZip.path()));
        assertTrue(Files.exists(pluginZip.unzippedPath()));
        assertEquals(1, pluginManager.getPlugins().size());
        assertEquals(pluginZip.pluginId(), pluginManager.idForPath(pluginZip.unzippedPath()));
    }

    @Test
    void loadNonExisting() {
        Path path = Paths.get("nonexisting");
        assertThrows(IllegalArgumentException.class, () ->
                pluginManager.loadPlugin(path));
    }

    @Test
    void loadTwiceFails() throws Exception {
        PluginZip pluginZip = new PluginZip.Builder(pluginsPath.resolve("my-plugin-1.2.3.zip"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();
        Path path = pluginZip.path();
        assertNotNull(pluginManager.loadPluginFromPath(path));
        assertThrows(PluginAlreadyLoadedException.class, () -> {
            pluginManager.loadPluginFromPath(path);
        });
    }

    @Test
    void loadPluginWithSameIdDifferentPathFails() throws Exception {
        String pluginId = "myPlugin";
        String pluginVersion = "1.2.3";
        Path plugin1Path = pluginsPath.resolve("my-plugin-1.2.3.zip");
        PluginZip plugin1 = new PluginZip.Builder(plugin1Path, pluginId)
                .pluginVersion(pluginVersion)
                .build();

        Path plugin2Path = pluginsPath.resolve("my-plugin-1.2.3-renamed.zip");
        PluginZip plugin2 = new PluginZip.Builder(plugin2Path, pluginId)
                .pluginVersion(pluginVersion)
                .build();

        // Verifique se o primeiro archbasePlugin com o id fornecido foi carregado
        assertNotNull(pluginManager.loadPluginFromPath(plugin1.path()));
        Path loadedPlugin1Path = pluginManager.getPlugin(pluginId).getPluginPath();
        Path path = plugin2.path();
        try {
            // Verifique se o segundo archbasePlugin não está carregado, pois tem os mesmos metadados
            pluginManager.loadPluginFromPath(path);
            fail("Espera-se que o loadPluginFromPath falhe");
        } catch (PluginRuntimeException e) {
            // Verifique se o caminho do archbasePlugin carregado permanece o mesmo
            PluginWrapper loadedPlugin = pluginManager.getPlugin(pluginId);
            assertThat(loadedPlugin.getPluginPath(), equalTo(loadedPlugin1Path));
            // Verifique se a mensagem inclui informações relevantes
            String message = e.getMessage();
            assertThat(message, startsWith("Já existe um plugin carregado"));
            assertThat(message, containsString(pluginId));
            assertThat(message, containsString("my-plugin-1.2.3-renamed"));
        }
    }

    /**
     * Este teste verifica o comportamento a partir de PF4J 2.x, onde plugins de diferentes
     * versões, mas com o pluginId não podem ser carregados corretamente porque a API
     * usa pluginId como o identificador único do archbasePlugin carregado.
     */
    @Test
    void loadPluginWithSameIdDifferentVersionsFails() throws Exception {
        String pluginId = "myPlugin";
        String plugin1Version = "1.2.3";
        Path plugin1Path = pluginsPath.resolve("my-plugin-1.2.3.zip");
        PluginZip plugin1 = new PluginZip.Builder(plugin1Path, pluginId)
                .pluginVersion(plugin1Version)
                .build();

        String plugin2Version = "2.0.0";
        Path plugin2Path = pluginsPath.resolve("my-plugin-2.0.0.zip");
        PluginZip plugin2 = new PluginZip.Builder(plugin2Path, pluginId)
                .pluginVersion(plugin2Version)
                .build();

        // Verifique se o primeiro archbasePlugin com o id fornecido foi carregado
        assertNotNull(pluginManager.loadPluginFromPath(plugin1.path()));
        Path loadedPlugin1Path = pluginManager.getPlugin(pluginId).getPluginPath();
        Path path = plugin2.path();
        try {
            // Verifique se o segundo archbasePlugin não está carregado, pois tem o mesmo pluginId
            PluginWrapper pluginWrapper = pluginManager.loadPluginFromPath(path);
            fail("Expected loadPluginFromPath to fail");
        } catch (PluginRuntimeException e) {
            // Verifique se o caminho e a versão do archbasePlugin carregado permanecem os mesmos
            PluginWrapper loadedPlugin = pluginManager.getPlugin(pluginId);
            assertThat(loadedPlugin.getPluginPath(), equalTo(loadedPlugin1Path));
            assertThat(loadedPlugin.getDescriptor().getVersion(), equalTo(plugin1Version));
        }
    }

    @Test
    void loadUnloadLoad() throws Exception {
        PluginZip pluginZip = new PluginZip.Builder(pluginsPath.resolve("my-plugin-1.2.3.zip"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();

        pluginManager.loadPlugins();

        assertEquals(1, pluginManager.getPlugins().size());
        assertTrue(pluginManager.unloadPlugin(pluginManager.idForPath(pluginZip.unzippedPath())));
        // verificação duplicada
        assertNull(pluginManager.idForPath(pluginZip.unzippedPath()));
        // Descarregamento duplo ok
        assertFalse(pluginManager.unloadPlugin(pluginManager.idForPath(pluginZip.unzippedPath())));
        assertNotNull(pluginManager.loadPlugin(pluginZip.unzippedPath()));
    }

    @Test
    void upgrade() throws Exception {
        String pluginId = "myPlugin";

        new PluginZip.Builder(pluginsPath.resolve("my-plugin-1.2.3.zip"), pluginId)
                .pluginVersion("1.2.3")
                .build();

        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        assertEquals(1, pluginManager.getPlugins().size());
        assertEquals(1, pluginManager.getStartedPlugins().size());

        PluginZip pluginZip2 = new PluginZip.Builder(pluginsPath.resolve("my-plugin-2.0.0.ZIP"), pluginId)
                .pluginVersion("2.0.0")
                .build();

        assertEquals("1.2.3", pluginManager.getPlugin(pluginId).getDescriptor().getVersion());

        pluginManager.unloadPlugin(pluginId);
        pluginManager.loadPlugin(pluginZip2.path()); // or `archbasePluginManager.loadPlugins();`
        pluginManager.startPlugin(pluginId);

        assertEquals(1, pluginManager.getPlugins().size());
        assertEquals("2.0.0", pluginManager.getPlugin(pluginId).getDescriptor().getVersion());
        assertEquals("2.0.0", pluginManager.getStartedPlugins().get(0).getDescriptor().getVersion());
    }

    @Test
    void getRoot() {
        assertEquals(pluginsPath, pluginManager.getPluginsRoot());
    }

    @Test
    void getRoots() {
        assertEquals(Collections.singletonList(pluginsPath), pluginManager.getPluginsRoots());
    }

    @Test
    void notAPlugin() {
        pluginsPath.resolve("not-a-zip");

        pluginManager.loadPlugins();

        assertEquals(0, pluginManager.getPlugins().size());
    }

    @Test
    void deletePlugin() throws Exception {
        PluginZip pluginZip1 = new PluginZip.Builder(pluginsPath.resolve("my-plugin-1.2.3.zip"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();

        PluginZip pluginZip3 = new PluginZip.Builder(pluginsPath.resolve("other-3.0.0.Zip"), "other")
                .pluginVersion("3.0.0")
                .build();

        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        assertEquals(2, pluginManager.getPlugins().size());

        pluginManager.deletePlugin(pluginZip1.pluginId());

        assertEquals(1, pluginManager.getPlugins().size());
        assertFalse(Files.exists(pluginZip1.path()));
        assertFalse(Files.exists(pluginZip1.unzippedPath()));
        assertTrue(Files.exists(pluginZip3.path()));
        assertTrue(Files.exists(pluginZip3.unzippedPath()));
    }

}
