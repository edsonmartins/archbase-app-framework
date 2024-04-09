package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.processor.ExtensionStorage;
import br.com.archbase.plugin.manager.processor.ServiceProviderExtensionStorage;
import br.com.archbase.plugin.manager.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * A implementação de base {@link java.util.ServiceLoader} para {@link ExtensionFinder}.
 * Esta classe procura extensões em todos os arquivos de índice de extensões {@code META-INF/services}.
 */
public class ServiceProviderExtensionFinder extends AbstractExtensionFinder {

    public static final String EXTENSIONS_RESOURCE = ServiceProviderExtensionStorage.EXTENSIONS_RESOURCE;
    private static final Logger log = LoggerFactory.getLogger(ServiceProviderExtensionFinder.class);

    public ServiceProviderExtensionFinder(ArchbasePluginManager archbasePluginManager) {
        super(archbasePluginManager);
    }

    @Override
    public Map<String, Set<String>> readClasspathStorages() {
        log.debug("Lendo armazenamentos de extensões do classpath");
        Map<String, Set<String>> result = new LinkedHashMap<>();

        final Set<String> bucket = new HashSet<>();
        try {
            Enumeration<URL> urls = getClass().getClassLoader().getResources(EXTENSIONS_RESOURCE);
            if (urls.hasMoreElements()) {
                collectExtensions(urls, bucket);
            } else {
                log.debug("Cannot find '{}'", EXTENSIONS_RESOURCE);
            }

            debugExtensions(bucket);

            result.put(null, bucket);
        } catch (IOException | URISyntaxException e) {
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
            log.debug("Lendo armazenamentos de extensões para o archbasePlugin '{}'", pluginId);
            final Set<String> bucket = new HashSet<>();

            try {
                Enumeration<URL> urls = ((PluginClassLoader) plugin.getPluginClassLoader()).findResources(EXTENSIONS_RESOURCE);
                if (urls.hasMoreElements()) {
                    collectExtensions(urls, bucket);
                } else {
                    log.debug("Não consigo encontrar '{}'", EXTENSIONS_RESOURCE);
                }

                debugExtensions(bucket);

                result.put(pluginId, bucket);
            } catch (IOException | URISyntaxException e) {
                log.error(e.getMessage(), e);
            }
        }

        return result;
    }

    private void collectExtensions(Enumeration<URL> urls, Set<String> bucket) throws URISyntaxException, IOException {
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            log.debug("Lendo '{}'", url.getFile());
            collectExtensions(url, bucket);
        }
    }

    private void collectExtensions(URL url, Set<String> bucket) throws URISyntaxException, IOException {
        Path extensionPath;

        if (url.toURI().getScheme().equals("jar")) {
            extensionPath = FileUtils.getPath(url.toURI(), EXTENSIONS_RESOURCE);
        } else {
            extensionPath = Paths.get(url.toURI());
        }

        try {
            bucket.addAll(readExtensions(extensionPath));
        } finally {
            FileUtils.closePath(extensionPath);
        }
    }

    private Set<String> readExtensions(Path extensionPath) throws IOException {
        final Set<String> result = new HashSet<>();
        Files.walkFileTree(extensionPath, Collections.<FileVisitOption>emptySet(), 1, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.debug("Lendo '{}'", file);
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    ExtensionStorage.read(reader, result);
                }

                return FileVisitResult.CONTINUE;
            }

        });

        return result;
    }

}
