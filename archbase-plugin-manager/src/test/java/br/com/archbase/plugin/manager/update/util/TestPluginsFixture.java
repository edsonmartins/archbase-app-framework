package br.com.archbase.plugin.manager.update.util;

import br.com.archbase.plugin.manager.update.PluginInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TestPluginsFixture {

    private static final String PLUGINS_JSON_FILE = "plugins.json";

    public static void setup() {
        FileWriter writer;
        try {
            writer = new FileWriter("downloads/" + PLUGINS_JSON_FILE);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao criar gravador para arquivo de manifesto de plug-ins", e);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<PluginInfo> plugins = new ArrayList<>();

        // plugin 1
        PluginInfo p1 = new PluginInfo();
        p1.id = "welcome-plugin";
        p1.description = "Welcome plugin";
        plugins.add(p1);
        // lançamentos para o plugin 1
        PluginInfo.PluginRelease p1r1 = new PluginInfo.PluginRelease();
        p1r1.version = "1.0.0";
        p1r1.date = new Date();
        p1r1.url = "archbase-demo-plugin1/1.0.0/plugin1-1.0.0.zip";
        p1.releases = Collections.singletonList(p1r1);

        // plugin 2
        PluginInfo p2 = new PluginInfo();
        p2.id = "hello-plugin";
        p2.description = "Hello plugin";
        plugins.add(p2);
        // lançamentos para o plugin 2
        PluginInfo.PluginRelease p2r1 = new PluginInfo.PluginRelease();
        p2r1.version = "1.0.0";
        p2r1.date = new Date();
        p2r1.url = "archbase-demo-plugin2/1.0.0/plugin2-1.0.0.zip";
        p2.releases = Collections.singletonList(p2r1);

        String json = gson.toJson(plugins);
        try {
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gravar o manifesto de plug-ins no sistema de arquivos", e);
        }
    }
}
