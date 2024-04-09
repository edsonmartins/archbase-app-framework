package br.com.archbase.plugin.manager.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Este filtro fornece lógica OR condicional em uma lista de filtros de arquivo.
 * Este filtro retorna {@code true} se um filtro na lista retornar {@code true}. Caso contrário, retorna {@code false}.
 * A verificação da lista de filtros de arquivos é interrompida quando o primeiro filtro retorna {@code true}.
 */
public class OrFileFilter implements FileFilter {

    /**
     * A lista de filtros de arquivo.
     */
    private List<FileFilter> fileFilters;

    public OrFileFilter() {
        this(new ArrayList<>());
    }

    public OrFileFilter(FileFilter... fileFilters) {
        this(Arrays.asList(fileFilters));
    }

    public OrFileFilter(List<FileFilter> fileFilters) {
        this.fileFilters = new ArrayList<>(fileFilters);
    }

    public OrFileFilter addFileFilter(FileFilter fileFilter) {
        fileFilters.add(fileFilter);

        return this;
    }

    public List<FileFilter> getFileFilters() {
        return Collections.unmodifiableList(fileFilters);
    }

    public void setFileFilters(List<FileFilter> fileFilters) {
        this.fileFilters = new ArrayList<>(fileFilters);
    }

    public boolean removeFileFilter(FileFilter fileFilter) {
        return fileFilters.remove(fileFilter);
    }

    @Override
    public boolean accept(File file) {
        if (this.fileFilters.isEmpty()) {
            return true;
        }

        for (FileFilter fileFilter : this.fileFilters) {
            if (fileFilter.accept(file)) {
                return true;
            }
        }

        return false;
    }

}
