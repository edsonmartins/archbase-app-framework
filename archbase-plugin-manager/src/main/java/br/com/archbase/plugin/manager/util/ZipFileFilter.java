package br.com.archbase.plugin.manager.util;

/**
 * Filtro de arquivo que aceita todos os arquivos que terminam com .ZIP.
 * Este filtro não diferencia maiúsculas de minúsculas.
 */
public class ZipFileFilter extends ExtensionFileFilter {

    /**
     * A extensão que este filtro irá pesquisar.
     */
    private static final String ZIP_EXTENSION = ".ZIP";

    public ZipFileFilter() {
        super(ZIP_EXTENSION);
    }

}
