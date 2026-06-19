package br.com.archbase.plugin.manager.update;


import br.com.archbase.plugin.manager.update.util.TestApplication;
import br.com.archbase.plugin.manager.update.util.TestPluginsFixture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class UpdateIntegrationTest {

    private WebServer webServer;
    private Path repositoryJson;

    @Before
    public void setup() throws Exception {
        Path downloadRoot = Files.createTempDirectory("archbase-plugin-repository");
        TestPluginsFixture.setup(downloadRoot);

        webServer = new WebServer()
                .setPort(0)
                .setResourceBase(downloadRoot.toAbsolutePath().toString());
        webServer.start();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        UpdateRepository repository = new DefaultUpdateRepository("localhost", new URL("http://localhost:" + webServer.getPort() + "/"));
        String json = gson.toJson(Collections.singletonList(repository));
        repositoryJson = Files.createTempFile("archbase-plugin-repositories", ".json");
        Files.writeString(repositoryJson, json, StandardCharsets.UTF_8);
    }

    @After
    public void tearDown() {
        webServer.shutdown();
    }

    @Test
    public void assertUpdateCreatesPlugins() {
        TestApplication subject = new TestApplication(repositoryJson);
        subject.start();

        assertEquals("Não espere nenhum plug-in carregado no início ", 0, subject.getPluginManager().getPlugins().size());

        subject.update();

        assertEquals("Espere dois plugins carregados na atualização", 2, subject.getPluginManager().getPlugins().size());
    }
}
