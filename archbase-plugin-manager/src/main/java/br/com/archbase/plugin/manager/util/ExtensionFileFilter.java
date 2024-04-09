package br.com.archbase.plugin.manager.util;

import java.io.File;
import java.io.FileFilter;

/**
 * O filtro aceita qualquer arquivo terminado em extensão. O caso do nome do arquivo é ignorado.
 */
public class ExtensionFileFilter implements FileFilter {

    private String extension;

    public ExtensionFileFilter(String extension) {
        this.extension = extension;
    }

    @Override
    public boolean accept(File file) {
        // executa uma verificação que não diferencia maiúsculas de minúsculas.
        return file.getName().toUpperCase().endsWith(extension.toUpperCase());
    }

}
