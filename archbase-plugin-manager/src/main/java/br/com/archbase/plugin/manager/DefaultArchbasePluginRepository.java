package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


public class DefaultArchbasePluginRepository extends BasePluginRepository {

    private static final Logger log = LoggerFactory.getLogger(DefaultArchbasePluginRepository.class);

    public DefaultArchbasePluginRepository(Path... pluginsRoots) {
        this(Arrays.asList(pluginsRoots));
    }

    public DefaultArchbasePluginRepository(List<Path> pluginsRoots) {
        super(pluginsRoots);

        AndFileFilter pluginsFilter = new AndFileFilter(new DirectoryFileFilter());
        pluginsFilter.addFileFilter(new NotFileFilter(createHiddenPluginFilter()));
        setFilter(pluginsFilter);
    }

    @Override
    public List<Path> getPluginPaths() {
        extractZipFiles();
        return super.getPluginPaths();
    }

    @Override
    public boolean deletePluginPath(Path pluginPath) {
        FileUtils.optimisticDelete(FileUtils.findWithEnding(pluginPath, ".zip", ".ZIP", ".Zip"));
        return super.deletePluginPath(pluginPath);
    }

    protected FileFilter createHiddenPluginFilter() {
        return new OrFileFilter(new HiddenFilter());
    }

    private void extractZipFiles() {
        // expandir arquivos zip de plug-ins
        pluginsRoots.stream()
                .flatMap(path -> streamFiles(path, new ZipFileFilter()))
                .map(File::toPath)
                .forEach(this::expandIfZip);
    }

    private void expandIfZip(Path filePath) {
        try {
            FileUtils.expandIfZip(filePath);
        } catch (IOException e) {
            log.error("Não é possível expandir o zip do archbasePlugin '{}'", filePath);
            log.error(e.getMessage(), e);
        }
    }

}
