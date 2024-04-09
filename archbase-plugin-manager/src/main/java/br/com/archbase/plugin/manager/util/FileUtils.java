package br.com.archbase.plugin.manager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public final class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
    }

    public static List<String> readLines(Path path, boolean ignoreComments) throws IOException {
        File file = path.toFile();
        if (!file.isFile()) {
            return new ArrayList<>();
        }

        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (ignoreComments && !line.startsWith("#") && !lines.contains(line)) {
                    lines.add(line);
                }
            }
        }

        return lines;
    }

    public static void writeLines(Collection<String> lines, Path path) throws IOException {
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    /**
     * Exclua um arquivo ou exclua recursivamente uma pasta, não siga os links simbólicos.
     *
     * @param path do arquivo ou pasta a ser excluído
     * @throws IOException se algo der errado
     */
    public static void delete(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isSymbolicLink()) {
                    Files.delete(path);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);

                return FileVisitResult.CONTINUE;
            }

        });
    }

    public static List<File> getJars(Path folder) {
        List<File> bucket = new ArrayList<>();
        getJars(bucket, folder);

        return bucket;
    }

    private static void getJars(final List<File> bucket, Path folder) {
        FileFilter jarFilter = new JarFileFilter();
        FileFilter directoryFilter = new DirectoryFileFilter();

        if (Files.isDirectory(folder)) {
            File[] jars = folder.toFile().listFiles(jarFilter);
            for (int i = 0; (jars != null) && (i < jars.length); ++i) {
                bucket.add(jars[i]);
            }

            File[] directories = folder.toFile().listFiles(directoryFilter);
            for (int i = 0; (directories != null) && (i < directories.length); ++i) {
                File directory = directories[i];
                getJars(bucket, directory.toPath());
            }
        }
    }

    /**
     * Encontra um caminho com várias terminações ou nulo se não for encontrado.
     *
     * @param basePath o nome base
     * @param endings  uma lista de terminações a serem pesquisadas
     * @return new path ou null se não for encontrado
     */
    public static Path findWithEnding(Path basePath, String... endings) {
        for (String ending : endings) {
            Path newPath = basePath.resolveSibling(basePath.getFileName() + ending);
            if (Files.exists(newPath)) {
                return newPath;
            }
        }

        return null;
    }

    /**
     * Exclua um arquivo (não recursivamente) e ignore quaisquer erros.
     *
     * @param path o caminho a ser excluído
     */
    public static void optimisticDelete(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.delete(path);
        } catch (IOException ignored) {
            // ignored
        }
    }

    /**
     * Descompacte um arquivo zip em um diretório com o mesmo nome do arquivo zip.
     * Por exemplo, se o arquivo zip for {@code my-plugin.zip}, o diretório resultante
     * é {@code my-plugin}.
     *
     * @param filePath o arquivo a ser avaliado
     * @return Caminho da pasta descompactada ou caminho original se este não for um arquivo zip
     * @throws IOException em caso de erro
     */
    public static Path expandIfZip(Path filePath) throws IOException {
        if (!isZipFile(filePath)) {
            return filePath;
        }

        FileTime pluginZipDate = Files.getLastModifiedTime(filePath);
        String fileName = filePath.getFileName().toString();
        String directoryName = fileName.substring(0, fileName.lastIndexOf("."));
        Path pluginDirectory = filePath.resolveSibling(directoryName);

        if (!Files.exists(pluginDirectory) || pluginZipDate.compareTo(Files.getLastModifiedTime(pluginDirectory)) > 0) {
            // expand '.zip' file
            Unzip unzip = new Unzip();
            unzip.setSource(filePath.toFile());
            unzip.setDestination(pluginDirectory.toFile());
            unzip.extract();
            log.info("ArchbasePlugin zip expandido '{}' em '{}'", filePath.getFileName(), pluginDirectory.getFileName());
        }

        return pluginDirectory;
    }

    /**
     * Retorna verdadeiro apenas se o caminho for um arquivo zip.
     *
     * @param path para um arquivo / diretório
     * @return true se o arquivo com final {@code .zip}
     */
    public static boolean isZipFile(Path path) {
        return Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".zip");
    }

    /**
     * Retorne verdadeiro apenas se o caminho for um arquivo jar.
     *
     * @param path para um arquivo / diretório
     * @return true se arquivo com finalização {@code .jar}
     */
    public static boolean isJarFile(Path path) {
        return Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".jar");
    }

    public static Path getPath(Path path, String first, String... more) throws IOException {
        URI uri = path.toUri();
        if (isJarFile(path)) {
            String pathString = path.toAbsolutePath().toString();
            // transformação para o sistema operacional Windows
            pathString = StringUtils.addStart(pathString.replace("\\", "/"), "/");
            // espaço é substituído por %20
            pathString = pathString.replace(" ", "%20");
            uri = URI.create("jar:file:" + pathString);
        }

        return getPath(uri, first, more);
    }

    public static Path getPath(URI uri, String first, String... more) throws IOException {
        return getFileSystem(uri).getPath(first, more);
    }

    public static void closePath(Path path) {
        if (path != null) {
            try {
                path.getFileSystem().close();
            } catch (Exception e) {
                // close silently
            }
        }
    }

    public static Path findFile(Path directoryPath, String fileName) {
        File[] files = directoryPath.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if (file.getName().equals(fileName)) {
                        return file.toPath();
                    }
                } else if (file.isDirectory()) {
                    Path foundFile = findFile(file.toPath(), fileName);
                    if (foundFile != null) {
                        return foundFile;
                    }
                }
            }
        }

        return null;
    }

    private static FileSystem getFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            return FileSystems.newFileSystem(uri, Collections.<String, String>emptyMap());
        }
    }
}
