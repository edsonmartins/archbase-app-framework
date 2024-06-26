package br.com.archbase.plugin.manager.update;

import br.com.archbase.plugin.manager.update.util.LenientDateTypeAdapter;
import br.com.archbase.plugin.manager.update.verifier.CompoundVerifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The default implementation of {@link UpdateRepository}.
 */
public class DefaultUpdateRepository implements UpdateRepository {

    private static final String DEFAULT_PLUGINS_JSON_FILENAME = "plugins.json";
    private static final Logger log = LoggerFactory.getLogger(DefaultUpdateRepository.class);

    private String id;
    private URL url;
    private String pluginsJsonFileName;

    private Map<String, PluginInfo> plugins;

    /**
     * Instantiates a new default update repository. The default plugins JSON file
     * name {@code plugins.json} will be used. Please use
     * {@link #DefaultUpdateRepository(String, URL, String)} if you want to choose
     * another file name than {@code plugins.json}}.
     *
     * @param id  the repository id
     * @param url the repository url
     */
    public DefaultUpdateRepository(String id, URL url) {
        this(id, url, DEFAULT_PLUGINS_JSON_FILENAME);
    }

    public DefaultUpdateRepository(String id, URL url, String pluginsJsonFileName) {
        this.id = id;
        this.url = url;
        this.pluginsJsonFileName = pluginsJsonFileName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Map<String, PluginInfo> getPlugins() {
        if (plugins == null) {
            initPlugins();
        }

        return plugins;
    }

    @Override
    public PluginInfo getPlugin(String id) {
        return getPlugins().get(id);
    }

    protected InputStream openURL(URL url) throws IOException {
        return url.openStream();
    }

    private void initPlugins() {
        Reader pluginsJsonReader;
        try {
            URL pluginsUrl = new URL(getUrl(), getPluginsJsonFileName());
            log.debug("Read plugins of '{}' repository from '{}'", id, pluginsUrl);
            pluginsJsonReader = new InputStreamReader(openURL(pluginsUrl));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            plugins = Collections.emptyMap();
            return;
        }

        Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new LenientDateTypeAdapter()).create();
        PluginInfo[] items = gson.fromJson(pluginsJsonReader, PluginInfo[].class);
        plugins = new HashMap<>(items.length);
        for (PluginInfo p : items) {
            if (p.releases != null) {
                for (PluginInfo.PluginRelease r : p.releases) {
                    try {
                        r.url = new URL(getUrl(), r.url).toString();
                        if (r.date.getTime() == 0) {
                            log.warn("Illegal release date when parsing {}@{}, setting to epoch", p.id, r.version);
                        }
                    } catch (MalformedURLException e) {
                        log.warn("Skipping release {} of plugin {} due to failure to build valid absolute URL. Url was {}{}", r.version, p.id, getUrl(), r.url);
                    }
                }
            }
            p.setRepositoryId(getId());
            plugins.put(p.id, p);
        }
        log.debug("Found {} plugins in repository '{}'", plugins.size(), id);
    }

    /**
     * Causes {@code plugins.json} to be read again to look for new updates from repositories.
     */
    @Override
    public void refresh() {
        plugins = null;
    }

    @Override
    public FileDownloader getFileDownloader() {
        return new SimpleFileDownloader();
    }

    /**
     * Gets a file verifier to execute on the downloaded file for it to be claimed valid.
     * May be a CompoundVerifier in order to chain several verifiers.
     *
     * @return list of {@link FileVerifier}s
     */
    @Override
    public FileVerifier getFileVerifier() {
        return new CompoundVerifier();
    }

    /**
     * Gets the plugins json file name. Returns {@code plugins.json} if null.
     *
     * @return the plugins json file name
     */
    public String getPluginsJsonFileName() {
        if (pluginsJsonFileName == null) {
            pluginsJsonFileName = DEFAULT_PLUGINS_JSON_FILENAME;
        }

        return pluginsJsonFileName;
    }

    /**
     * Choose another file name than {@code plugins.json}.
     *
     * @param pluginsJsonFileName the name (relative) of plugins.json file.
     *                            If null, will default to {@code plugins.json}
     */
    public void setPluginsJsonFileName(String pluginsJsonFileName) {
        this.pluginsJsonFileName = pluginsJsonFileName;
    }

}