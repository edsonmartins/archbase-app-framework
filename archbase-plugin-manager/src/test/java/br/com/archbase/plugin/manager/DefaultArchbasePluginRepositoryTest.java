package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.PluginZip;
import br.com.archbase.plugin.manager.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultArchbasePluginRepositoryTest {

    Path pluginsPath1;
    Path pluginsPath2;

    @BeforeEach
    void setUp() throws IOException {
        pluginsPath1 = Files.createTempDirectory("junit-archbase-");
        pluginsPath2 = Files.createTempDirectory("junit-archbase-");
        Path plugin1Path = Files.createDirectory(pluginsPath1.resolve("plugin-1"));
        // Prove que podemos deletar uma pasta com um arquivo dentro
        Files.createFile(plugin1Path.resolve("myfile"));
        // Crie um arquivo zip para o archbasePlugin-1 para testar se ele é excluído quando o archbasePlugin é excluído
        new PluginZip.Builder(pluginsPath1.resolve("plugin-1.zip"), "plugin-1").pluginVersion("1.0").build();
        Files.createDirectory(pluginsPath2.resolve("plugin-2"));
        Files.createDirectory(pluginsPath2.resolve("plugin-3"));
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.delete(pluginsPath1);
        FileUtils.delete(pluginsPath2);
    }

    /**
     * Teste para o método {@link DefaultArchbasePluginRepository#getPluginPaths()}.
     */
    @Test
    void testGetPluginArchivesFromSinglePath() {
        PluginRepository repository = new DefaultArchbasePluginRepository(pluginsPath2);

        List<Path> pluginPaths = repository.getPluginPaths();

        assertEquals(2, pluginPaths.size());
        assertPathExists(pluginPaths, pluginsPath2.resolve("plugin-2"));
        assertPathExists(pluginPaths, pluginsPath2.resolve("plugin-3"));
    }

    /**
     * Teste para o método {@link DefaultArchbasePluginRepository#getPluginPaths()}.
     */
    @Test
    void testGetPluginArchives() {
        PluginRepository repository = new DefaultArchbasePluginRepository(pluginsPath1, pluginsPath2);

        List<Path> pluginPaths = repository.getPluginPaths();

        assertEquals(3, pluginPaths.size());
        assertPathExists(pluginPaths, pluginsPath1.resolve("plugin-1"));
        assertPathExists(pluginPaths, pluginsPath2.resolve("plugin-2"));
        assertPathExists(pluginPaths, pluginsPath2.resolve("plugin-3"));
    }

    /**
     * Teste para o método {@link DefaultArchbasePluginRepository#deletePluginPath(Path)}.
     */
    @Test
    void testDeletePluginPath() {
        PluginRepository repository = new DefaultArchbasePluginRepository(pluginsPath1, pluginsPath2);

        assertTrue(Files.exists(pluginsPath1.resolve("plugin-1.zip")));
        assertTrue(repository.deletePluginPath(pluginsPath1.resolve("plugin-1")));
        assertFalse(Files.exists(pluginsPath1.resolve("plugin-1.zip")));
        assertTrue(repository.deletePluginPath(pluginsPath2.resolve("plugin-3")));
        assertFalse(repository.deletePluginPath(pluginsPath2.resolve("plugin-4")));

        List<Path> pluginPaths = repository.getPluginPaths();

        assertEquals(1, pluginPaths.size());
        assertEquals("plugin-2", pluginsPath2.relativize(pluginPaths.get(0)).toString());
    }

    private void assertPathExists(List<Path> paths, Path path) {
        assertTrue(paths.contains(path), "O diretório deve conter o arquivo " + path);
    }

}
