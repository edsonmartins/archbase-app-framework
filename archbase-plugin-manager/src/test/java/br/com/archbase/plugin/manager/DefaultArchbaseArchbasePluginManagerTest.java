package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.PluginJar;
import br.com.archbase.plugin.manager.plugin.PluginZip;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultArchbaseArchbasePluginManagerTest {

    @TempDir
    Path pluginsPath;
    private DefaultArchbasePluginManager pluginManager;
    private DefaultArchbasePluginDescriptor pluginDescriptor;
    private PluginWrapper pluginWrapper;

    @BeforeEach
    void setUp() throws IOException {
        pluginManager = new DefaultArchbasePluginManager(pluginsPath);

        pluginDescriptor = new DefaultArchbasePluginDescriptor();
        pluginDescriptor.setPluginId("myPlugin");
        pluginDescriptor.setPluginVersion("1.2.3");
        pluginDescriptor.setPluginDescription("My archbasePlugin");
        pluginDescriptor.setDependencies("bar, baz");
        pluginDescriptor.setProvider("Me");
        pluginDescriptor.setRequires("5.0.0");

        pluginWrapper = new PluginWrapper(pluginManager, pluginDescriptor, Files.createTempDirectory("test"), getClass().getClassLoader());
    }

    @AfterEach
    void tearDown() {
        pluginManager = null;
        pluginDescriptor = null;
        pluginWrapper = null;
    }

    @Test
    void validateOK() {
        pluginManager.validatePluginDescriptor(pluginDescriptor);
    }

    @Test
    void validateFailsOnId() {
        pluginDescriptor.setPluginId("");
        assertThrows(PluginRuntimeException.class, () -> pluginManager.validatePluginDescriptor(pluginDescriptor));
    }

    @Test
    void validateFailsOnVersion() {
        pluginDescriptor.setPluginVersion(null);
        assertThrows(PluginRuntimeException.class, () -> pluginManager.validatePluginDescriptor(pluginDescriptor));
    }

    @Test
    void validateNoPluginClass() {
        pluginManager.validatePluginDescriptor(pluginDescriptor);
        assertEquals(ArchbasePlugin.class.getName(), pluginDescriptor.getPluginClass());
    }

    @Test
    void isPluginValid() {
        // Por padrão, aceita tudo, pois a versão do sistema não foi fornecida
        assertTrue(pluginManager.isPluginValid(pluginWrapper));

        pluginManager.setSystemVersion("1.0.0");
        assertFalse(pluginManager.isPluginValid(pluginWrapper));

        pluginManager.setSystemVersion("5.0.0");
        assertTrue(pluginManager.isPluginValid(pluginWrapper));

        pluginManager.setSystemVersion("6.0.0");
        assertTrue(pluginManager.isPluginValid(pluginWrapper));
    }

    @Test
    void isPluginValidAllowExact() {
        pluginManager.setExactVersionAllowed(true);

        // Por padrão, aceita tudo, pois a versão do sistema não foi fornecida
        assertTrue(pluginManager.isPluginValid(pluginWrapper));

        pluginManager.setSystemVersion("1.0.0");
        assertFalse(pluginManager.isPluginValid(pluginWrapper));

        pluginManager.setSystemVersion("5.0.0");
        assertTrue(pluginManager.isPluginValid(pluginWrapper));

        pluginManager.setSystemVersion("6.0.0");
        assertFalse(pluginManager.isPluginValid(pluginWrapper));
    }

    @Test
    void testDefaultExactVersionAllowed() {
        assertFalse(pluginManager.isExactVersionAllowed());
    }

    /**
     * Teste se um archbasePlugin desabilitado não inicia.
     */
    @Test
    void testPluginDisabledNoStart() throws IOException {
        new PluginZip.Builder(pluginsPath.resolve("my-plugin-1.2.3.zip"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();

        final PluginStatusProvider statusProvider = mock(PluginStatusProvider.class);
        when(statusProvider.isPluginDisabled("myPlugin")).thenReturn(true);

        ArchbasePluginManager archbasePluginManager = new DefaultArchbasePluginManager(pluginsPath) {

            protected PluginStatusProvider createPluginStatusProvider() {
                return statusProvider;
            }

        };

        archbasePluginManager.loadPlugins();
        archbasePluginManager.startPlugins();

        assertEquals(1, archbasePluginManager.getPlugins().size());
        assertEquals(0, archbasePluginManager.getStartedPlugins().size());

        PluginWrapper plugin = archbasePluginManager.getPlugin("myPlugin");
        assertSame(PluginState.DISABLED, plugin.getPluginState());
    }

    @Test
    void deleteZipPlugin() throws Exception {
        PluginZip pluginZip = new PluginZip.Builder(pluginsPath.resolve("my-archbasePlugin-1.2.3.zip"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();

        pluginManager.loadPlugin(pluginZip.path());
        pluginManager.startPlugin(pluginZip.pluginId());

        assertEquals(1, pluginManager.getPlugins().size());

        boolean deleted = pluginManager.deletePlugin(pluginZip.pluginId());
        assertTrue(deleted);

        assertFalse(pluginZip.file().exists());
    }

    @Test
    void deleteJarPlugin() throws Exception {
        PluginJar pluginJar = new PluginJar.Builder(pluginsPath.resolve("my-archbasePlugin-1.2.3.jar"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();

        pluginManager.loadPlugin(pluginJar.path());
        pluginManager.startPlugin(pluginJar.pluginId());

        assertEquals(1, pluginManager.getPlugins().size());

        boolean deleted = pluginManager.deletePlugin(pluginJar.pluginId());
        assertTrue(deleted);

        assertFalse(pluginJar.file().exists());
    }

}
