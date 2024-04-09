package br.com.archbase.plugin.manager.util;

import java.io.File;
import java.io.FileFilter;

/**
 * Filtro que aceita apenas arquivos ocultos.
 */
public class HiddenFilter implements FileFilter {

    @Override
    public boolean accept(File file) {
        return file.isHidden();
    }

}
