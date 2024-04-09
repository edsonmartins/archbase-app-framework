package br.com.archbase.plugin.manager.util;

import java.io.File;
import java.io.FileFilter;

/**
 * O filtro aceita arquivos que são diretórios.
 */
public class DirectoryFileFilter implements FileFilter {

    @Override
    public boolean accept(File file) {
        return file.isDirectory();
    }

}
