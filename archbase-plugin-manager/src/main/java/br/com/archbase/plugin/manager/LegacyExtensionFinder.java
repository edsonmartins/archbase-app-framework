package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.processor.ExtensionStorage;
import br.com.archbase.plugin.manager.processor.LegacyExtensionStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Todas as extensões declaradas em um archbasePlugin são indexadas em um arquivo {@code META-INF / extensions.idx}.
 * Esta classe procura extensões em todos os arquivos de índice de extensões {@code META-INF / extensions.idx}.
 */
public class LegacyExtensionFinder extends AbstractExtensionFinder {

    public static final String EXTENSIONS_RESOURCE = LegacyExtensionStorage.EXTENSIONS_RESOURCE;
    private static final Logger log = LoggerFactory.getLogger(LegacyExtensionFinder.class);

    public LegacyExtensionFinder(ArchbasePluginManager archbasePluginManager) {
        super(archbasePluginManager);
    }

    @Override
    public Map<String, Set<String>> readClasspathStorages() {
        log.debug("Lendo armazenamentos de extensões do classpath");
        Map<String, Set<String>> result = new LinkedHashMap<>();

        Set<String> bucket = new HashSet<>();
        try {
            Enumeration<URL> urls = getClass().getClassLoader().getResources(EXTENSIONS_RESOURCE);
            if (urls.hasMoreElements()) {
                collectExtensions(urls, bucket);
            } else {
                log.debug("Não consigo encontrar'{}'", EXTENSIONS_RESOURCE);
            }

            debugExtensions(bucket);

            result.put(null, bucket);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return result;
    }

    @Override
    public Map<String, Set<String>> readPluginsStorages() {
        log.debug("Lendo armazenamentos de extensões de plug-ins");
        Map<String, Set<String>> result = new LinkedHashMap<>();

        List<PluginWrapper> plugins = archbasePluginManager.getPlugins();
        for (PluginWrapper plugin : plugins) {
            String pluginId = plugin.getDescriptor().getPluginId();
            log.debug("Leitura de armazenamento de extensões do archbasePlugin '{}'", pluginId);
            Set<String> bucket = new HashSet<>();

            try {
                log.debug("Lendo '{}'", EXTENSIONS_RESOURCE);
                ClassLoader pluginClassLoader = plugin.getPluginClassLoader();
                try (InputStream resourceStream = pluginClassLoader.getResourceAsStream(EXTENSIONS_RESOURCE)) {
                    if (resourceStream == null) {
                        log.debug("Cannot find '{}'", EXTENSIONS_RESOURCE);
                    } else {
                        collectExtensions(resourceStream, bucket);
                    }
                }

                debugExtensions(bucket);

                result.put(pluginId, bucket);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        return result;
    }

    private void collectExtensions(Enumeration<URL> urls, Set<String> bucket) throws IOException {
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            log.debug("Lendo '{}'", url.getFile());
            collectExtensions(url.openStream(), bucket);
        }
    }

    private void collectExtensions(InputStream inputStream, Set<String> bucket) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            ExtensionStorage.read(reader, bucket);
        }
    }

}
