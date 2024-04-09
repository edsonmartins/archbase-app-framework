package br.com.archbase.plugin.manager.util;

import br.com.archbase.plugin.manager.plugin.PluginZip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    @TempDir
    Path pluginsPath;

    @Test
    void expandIfZipForZipWithOnlyModuleDescriptor() throws Exception {
        PluginZip pluginZip = new PluginZip.Builder(pluginsPath.resolve("my-plugin-1.2.3.zip"), "myPlugin")
                .pluginVersion("1.2.3")
                .build();

        Path unzipped = FileUtils.expandIfZip(pluginZip.path());
        assertEquals(pluginZip.unzippedPath(), unzipped);
        assertTrue(Files.exists(unzipped.resolve("plugin.properties")));
    }

    @Test
    void expandIfZipForZipWithResourceFile() throws Exception {
        PluginZip pluginZip = new PluginZip.Builder(pluginsPath.resolve("my-second-plugin-1.2.3.zip"), "myPlugin")
                .pluginVersion("1.2.3")
                .addFile(Paths.get("classes/META-INF/plugin-file"), "plugin")
                .build();

        Path unzipped = FileUtils.expandIfZip(pluginZip.path());
        assertEquals(pluginZip.unzippedPath(), unzipped);
        assertTrue(Files.exists(unzipped.resolve("classes/META-INF/plugin-file")));
    }

    @Test
    void expandIfZipNonZipFiles() throws Exception {
        // Arquivo sem .suffix
        Path extra = pluginsPath.resolve("extra");
        assertEquals(extra, FileUtils.expandIfZip(extra));

        // Pasta
        Path folder = pluginsPath.resolve("folder");
        assertEquals(folder, FileUtils.expandIfZip(folder));
    }

}
