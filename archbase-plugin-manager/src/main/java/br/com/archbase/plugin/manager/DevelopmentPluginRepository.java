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

public class DevelopmentPluginRepository extends BasePluginRepository {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentPluginRepository.class);

    public static final String MAVEN_BUILD_DIR = "target";
    public static final String GRADLE_BUILD_DIR = "build";

    public DevelopmentPluginRepository(Path... pluginsRoots) {
        this(Arrays.asList(pluginsRoots));
    }

    public DevelopmentPluginRepository(List<Path> pluginsRoots) {
        super(pluginsRoots);

        AndFileFilter pluginsFilter = new AndFileFilter(new DirectoryFileFilter());
        pluginsFilter.addFileFilter(new NotFileFilter(createHiddenPluginFilter()));
        setFilter(pluginsFilter);
    }

    protected FileFilter createHiddenPluginFilter() {
        OrFileFilter hiddenPluginFilter = new OrFileFilter(new HiddenFilter());

        // pula as pastas de saída de construção padrão, pois elas causarão erros nos registros
        hiddenPluginFilter
                .addFileFilter(new NameFileFilter(MAVEN_BUILD_DIR))
                .addFileFilter(new NameFileFilter(GRADLE_BUILD_DIR));

        return hiddenPluginFilter;
    }

    @Override
    public List<Path> getPluginsPaths() {
        extractZipFiles();
        return super.getPluginsPaths();
    }

    private void extractZipFiles() {
        // expandir arquivos zip de plug-ins
        File[] zipFiles = pluginsRoot.toFile().listFiles(new ZipFileFilter());
        if ((zipFiles != null) && zipFiles.length > 0) {
            for (File pluginZip : zipFiles) {
                try {
                    FileUtils.expandIfZip(pluginZip.toPath());
                } catch (IOException e) {
                    log.error("Não é possível expandir o zip do plugin '{}'", pluginZip);
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

}
