package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class BasePluginRepository implements PluginRepository {

    protected List<Path> pluginsRoots;
    protected Path pluginsRoot;

    protected FileFilter filter;
    protected Comparator<File> comparator;

    public BasePluginRepository(Path... pluginsRoots) {
        this(Arrays.asList(pluginsRoots));
    }

    public BasePluginRepository(Path pluginsRoot) {
        this(pluginsRoot, null);
    }

    public BasePluginRepository(Path pluginsRoot, FileFilter filter) {
        this.pluginsRoot = pluginsRoot;
        this.filter = filter;

        // last modified file is first
        this.comparator = (o1, o2) -> (int) (o2.lastModified() - o1.lastModified());
    }

    public BasePluginRepository(List<Path> pluginsRoots) {
        this(pluginsRoots, null);
    }

    public BasePluginRepository(List<Path> pluginsRoots, FileFilter filter) {
        this.pluginsRoots = pluginsRoots;
        this.filter = filter;

        // o último arquivo modificado é o primeiro
        this.comparator = Comparator.comparingLong(File::lastModified);
    }

    public void setFilter(FileFilter filter) {
        this.filter = filter;
    }

    /**
     * Defina um {@link File} {@link Comparator} usado para classificar os arquivos listados em {@code pluginsRoot}.
     * Este comparador é usado no método {@link #getPluginPaths ()}.
     * Por padrão, é usado um comparador de arquivo que retorna os últimos arquivos modificados primeiro.
     * Se você não quiser um comparador de arquivo, chame este método com {@code null}.
     */
    public void setComparator(Comparator<File> comparator) {
        this.comparator = comparator;
    }

    @Override
    public List<Path> getPluginPaths() {
        return pluginsRoots.stream()
                .flatMap(path -> streamFiles(path, filter))
                .sorted(comparator)
                .map(File::toPath)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deletePluginPath(Path pluginPath) {
        if (!filter.accept(pluginPath.toFile())) {
            return false;
        }

        try {
            FileUtils.delete(pluginPath);
            return true;
        } catch (NoSuchFileException e) {
            return false;
        } catch (IOException e) {
            throw new PluginRuntimeException(e);
        }
    }

    protected Stream<File> streamFiles(Path directory, FileFilter filter) {
        File[] files = directory.toFile().listFiles(filter);
        return files != null
                ? Arrays.stream(files)
                : Stream.empty();
    }

    @Override
    public List<Path> getPluginsPaths() {
        File[] files = pluginsRoot.toFile().listFiles(filter);

        if ((files == null) || files.length == 0) {
            return Collections.emptyList();
        }

        if (comparator != null) {
            Arrays.sort(files, comparator);
        }

        List<Path> paths = new ArrayList<>(files.length);
        for (File file : files) {
            paths.add(file.toPath());
        }

        return paths;
    }

}
