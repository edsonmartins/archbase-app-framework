package br.com.archbase.plugin.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


class DevelopmentPluginRepositoryTest {

    @TempDir
    Path pluginsPath;

    @BeforeEach
    void setUp() throws IOException {
        // pasta bin padrão maven / gradle - devem ser ignorados no modo de desenvolvimento porque a causa dos erros
        Files.createDirectory(pluginsPath.resolve(DevelopmentPluginRepository.MAVEN_BUILD_DIR));
        Files.createDirectory(pluginsPath.resolve(DevelopmentPluginRepository.GRADLE_BUILD_DIR));
    }

    @Test
    void testGetPluginArchivesInDevelopmentMode() {
        PluginRepository repository = new DevelopmentPluginRepository(pluginsPath);

        List<Path> pluginPaths = repository.getPluginPaths();

        // destino e construção devem ser ignorados
        assertEquals(0, pluginPaths.size());
        assertPathDoesNotExists(pluginPaths, pluginsPath.resolve(DevelopmentPluginRepository.MAVEN_BUILD_DIR));
        assertPathDoesNotExists(pluginPaths, pluginsPath.resolve(DevelopmentPluginRepository.GRADLE_BUILD_DIR));
    }

    private void assertPathDoesNotExists(List<Path> paths, Path path) {
        assertFalse(paths.contains(path), "O diretório não deve conter o arquivo " + path);
    }

}
