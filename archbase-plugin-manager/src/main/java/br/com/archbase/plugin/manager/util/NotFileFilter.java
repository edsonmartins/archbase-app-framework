package br.com.archbase.plugin.manager.util;

import java.io.File;
import java.io.FileFilter;

/**
 * Este filtro produz um NÃO lógico dos filtros especificados.
 */
public class NotFileFilter implements FileFilter {

    private FileFilter filter;

    public NotFileFilter(FileFilter filter) {
        this.filter = filter;
    }

    @Override
    public boolean accept(File file) {
        return !filter.accept(file);
    }

}
