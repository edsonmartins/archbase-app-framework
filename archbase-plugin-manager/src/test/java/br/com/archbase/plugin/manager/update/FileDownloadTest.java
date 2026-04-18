package br.com.archbase.plugin.manager.update;

import br.com.archbase.plugin.manager.PluginRuntimeException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Teste download arquivo
 */
public class FileDownloadTest {

    private SimpleFileDownloader downloader;
    private Server webserver;
    private Path updateRepoDir;
    private Path repoFile;
    private Path emptyFile;

    @Before
    public void setup() throws IOException {
        downloader = new SimpleFileDownloader();
        updateRepoDir = Files.createTempDirectory("repo");
        updateRepoDir.toFile().deleteOnExit();
        repoFile = Files.createFile(updateRepoDir.resolve("myfile"));
        BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(repoFile, Charset.forName("utf-8"), StandardOpenOption.APPEND));
        writer.write("test");
        writer.close();
        emptyFile = Files.createFile(updateRepoDir.resolve("emptyFile"));
    }

    @After
    public void tearDown() throws Exception {
        if (webserver != null) {
            webserver.stop();
        }
    }

    @Test
    public void downloadLocal() throws Exception {
        assertTrue(Files.exists(repoFile));
        Path downloaded = downloader.downloadFile(repoFile.toUri().toURL());
        assertTrue(Files.exists(downloaded));
        // O arquivo ainda permanece no local original
        assertTrue(Files.exists(repoFile));
        // Os atributos do arquivo são copiados
        assertEquals(repoFile.toFile().lastModified(), downloaded.toFile().lastModified());
        Files.delete(downloaded);
        assertTrue(Files.exists(repoFile));
    }

    @Test
    public void downloadHttp() throws Exception {
        webserver = new Server(5500);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirAllowed(true);
        resourceHandler.setBaseResource(ResourceFactory.root().newResource(updateRepoDir.toAbsolutePath()));

        webserver.setHandler(resourceHandler);
        webserver.start();

        URL downloadUrl = new URL("http://localhost:5500/myfile");
        Path downloaded = downloader.downloadFile(downloadUrl);
        assertTrue(Files.exists(downloaded));
        assertEquals(4, Files.size(downloaded));
        // Os atributos do arquivo são copiados
        assertEquals(downloadUrl.openConnection().getLastModified(), downloaded.toFile().lastModified());
    }

    @Test(expected = PluginRuntimeException.class)
    public void unsupportedProtocol() throws Exception {
        downloader.downloadFile(new URL("jar:file:!/myfile.jar"));
    }


}
