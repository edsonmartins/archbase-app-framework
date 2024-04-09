package br.com.archbase.plugin.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class JarPluginRepositoryTest {

    @TempDir
    Path pluginsPath;

    /**
     * Teste para o método {@link JarPluginRepository#deletePluginPath(Path)}.
     */
    @Test
    void testDeletePluginPath() throws Exception {
        PluginRepository repository = new JarPluginRepository(pluginsPath);

        Path plugin1Path = Files.createDirectory(pluginsPath.resolve("archbasePlugin-1"));
        Path plugin1JarPath = Files.createFile(pluginsPath.resolve("archbasePlugin-1.jar"));

        assertFalse(repository.deletePluginPath(plugin1Path));

        List<Path> pluginPaths = repository.getPluginPaths();
        assertEquals(1, pluginPaths.size());

        assertTrue(repository.deletePluginPath(plugin1JarPath));

        pluginPaths = repository.getPluginPaths();
        assertEquals(0, pluginPaths.size());
    }

}
