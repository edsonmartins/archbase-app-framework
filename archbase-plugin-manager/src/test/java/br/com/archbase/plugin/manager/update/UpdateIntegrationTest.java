package br.com.archbase.plugin.manager.update;


import br.com.archbase.plugin.manager.update.util.TestApplication;
import br.com.archbase.plugin.manager.update.util.TestPluginsFixture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UpdateIntegrationTest {

    private WebServer webServer;

    @Before
    public void setup() throws Exception {
        TestPluginsFixture.setup();

        FileWriter writer = new FileWriter("repositories.json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<UpdateRepository> repositories = new ArrayList<UpdateRepository>();
        repositories.add(new DefaultUpdateRepository("localhost", new URL("http://localhost:8081/")));

        String json = gson.toJson(repositories);
        writer.write(json);
        writer.close();

        webServer = new WebServer();
        webServer.start();
    }

    @After
    public void tearDown() {
        webServer.shutdown();
    }

    @Test
    public void assertUpdateCreatesPlugins() {
        TestApplication subject = new TestApplication();
        subject.start();

        assertEquals("Não espere nenhum plug-in carregado no início ", 0, subject.getPluginManager().getPlugins().size());

        subject.update();

        assertEquals("Espere dois plugins carregados na atualização", 2, subject.getPluginManager().getPlugins().size());
    }
}
