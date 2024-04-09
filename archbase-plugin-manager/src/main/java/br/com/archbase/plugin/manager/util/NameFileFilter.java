package br.com.archbase.plugin.manager.util;

import java.io.File;
import java.io.FileFilter;

/**
 * O filtro aceita qualquer arquivo com este nome. O caso do nome do arquivo é ignorado.
 */
public class NameFileFilter implements FileFilter {

    private String name;

    public NameFileFilter(String name) {
        this.name = name;
    }

    @Override
    public boolean accept(File file) {
        // executa uma verificação que não diferencia maiúsculas de minúsculas.
        return file.getName().equalsIgnoreCase(name);
    }

}
