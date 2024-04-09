package br.com.archbase.plugin.manager.util;

/**
 * Filtro de arquivo que aceita todos os arquivos que terminam com .JAR.
 * Este filtro não diferencia maiúsculas de minúsculas.
 */
public class JarFileFilter extends ExtensionFileFilter {

    /**
     * A extensão que este filtro irá pesquisar.
     */
    private static final String JAR_EXTENSION = ".JAR";

    public JarFileFilter() {
        super(JAR_EXTENSION);
    }

}
