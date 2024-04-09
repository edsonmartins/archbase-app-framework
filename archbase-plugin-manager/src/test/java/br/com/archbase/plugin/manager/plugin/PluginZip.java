package br.com.archbase.plugin.manager.plugin;

import br.com.archbase.plugin.manager.PropertiesPluginDescriptorFinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Representa um arquivo {@code zip} de plugin.
 * O arquivo {@code plugin.properties} é criado imediatamente a partir das informações fornecidas no {@link Builder}.
 */
public class PluginZip {

    private final Path path;
    private final String pluginId;
    private final String pluginClass;
    private final String pluginVersion;

    protected PluginZip(Builder builder) {
        this.path = builder.path;
        this.pluginId = builder.pluginId;
        this.pluginClass = builder.pluginClass;
        this.pluginVersion = builder.pluginVersion;
    }

    public static Properties createProperties(Map<String, String> map) {
        Properties properties = new Properties();
        properties.putAll(map);

        return properties;
    }

    public Path path() {
        return path;
    }

    public File file() {
        return path.toFile();
    }

    public String pluginId() {
        return pluginId;
    }

    public String pluginClass() {
        return pluginClass;
    }

    public String pluginVersion() {
        return pluginVersion;
    }

    public Path unzippedPath() {
        Path path = path();
        String fileName = path.getFileName().toString();

        return path.getParent().resolve(fileName.substring(0, fileName.length() - 4)); // sem sufixo ".zip"
    }

    public static class Builder {

        private final Path path;
        private final String pluginId;

        private String pluginClass;
        private String pluginVersion;
        private Map<String, String> properties = new LinkedHashMap<>();
        private Map<Path, byte[]> files = new LinkedHashMap<>();

        public Builder(Path path, String pluginId) {
            this.path = path;
            this.pluginId = pluginId;
        }

        public Builder pluginClass(String pluginClass) {
            this.pluginClass = pluginClass;

            return this;
        }

        public Builder pluginVersion(String pluginVersion) {
            this.pluginVersion = pluginVersion;

            return this;
        }

        /**
         * Adicione propriedades extras ao arquivo {@code properties}.
         * Como nome de atributo possível, consulte {@link PropertiesPluginDescriptorFinder}.
         */
        public Builder properties(Map<String, String> properties) {
            this.properties.putAll(properties);

            return this;
        }

        /**
         * Adicione uma propriedade extra ao arquivo {@code properties}.
         * Como nome de propriedade possível, consulte {@link PropertiesPluginDescriptorFinder}.
         */
        public Builder property(String name, String value) {
            properties.put(name, value);

            return this;
        }

        /**
         * Adiciona um arquivo ao arquivo.
         *
         * @param path    o caminho relativo do arquivo
         * @param content o conteúdo do arquivo
         */
        public Builder addFile(Path path, byte[] content) {
            files.put(path, content.clone());

            return this;
        }

        /**
         * Adiciona um arquivo ao arquivo.
         *
         * @param path    o caminho relativo do arquivo
         * @param content o conteúdo do arquivo
         */
        public Builder addFile(Path path, String content) {
            files.put(path, content.getBytes());

            return this;
        }

        public PluginZip build() throws IOException {
            createPropertiesFile();

            return new PluginZip(this);
        }

        protected void createPropertiesFile() throws IOException {
            Map<String, String> map = new LinkedHashMap<>();
            map.put(PropertiesPluginDescriptorFinder.PLUGIN_ID, pluginId);
            map.put(PropertiesPluginDescriptorFinder.PLUGIN_VERSION, pluginVersion);
            if (pluginClass != null) {
                map.put(PropertiesPluginDescriptorFinder.PLUGIN_CLASS, pluginClass);
            }
            if (properties != null) {
                map.putAll(properties);
            }

            try (ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(path.toFile()))) {
                ZipEntry propertiesFile = new ZipEntry(PropertiesPluginDescriptorFinder.DEFAULT_PROPERTIES_FILE_NAME);
                outputStream.putNextEntry(propertiesFile);
                createProperties(map).store(outputStream, "");
                outputStream.closeEntry();

                for (Map.Entry<Path, byte[]> fileEntry : files.entrySet()) {
                    ZipEntry file = new ZipEntry(fileEntry.getKey().toString());
                    outputStream.putNextEntry(file);
                    outputStream.write(fileEntry.getValue());
                    outputStream.closeEntry();
                }
            }
        }

    }

}
