package br.com.archbase.plugin.manager.asm;

import br.com.archbase.plugin.manager.Extension;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Esta classe contém os parâmetros de uma {@link Extension}
 * anotação definida para uma determinada classe.
 */
public final class ExtensionInfo {

    private static final Logger log = LoggerFactory.getLogger(ExtensionInfo.class);

    private final String className;

    int ordinal = 0;
    List<String> plugins = new ArrayList<>();
    List<String> points = new ArrayList<>();

    private ExtensionInfo(String className) {
        this.className = className;
    }

    /**
     * Carregue um {@link ExtensionInfo} para uma determinada classe.
     *
     * @param className   nome absoluto da classe
     * @param classLoader carregador de classes para acessar a classe
     * @return o {@link ExtensionInfo}, se a classe foi anotada com uma {@link Extension}, caso contrário, nulo
     */
    public static ExtensionInfo load(String className, ClassLoader classLoader) {
        try (InputStream input = classLoader.getResourceAsStream(className.replace('.', '/') + ".class")) {
            ExtensionInfo info = new ExtensionInfo(className);
            new ClassReader(input).accept(new ExtensionVisitor(info), ClassReader.SKIP_DEBUG);

            return info;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtenha o nome da classe para a qual as informações de extensão foram criadas.
     *
     * @return nome absoluto da classe
     */
    public String getClassName() {
        return className;
    }

    /**
     * Obtenha o valor {@link Extension # ordinal ()}, que foi atribuído à extensão.
     *
     * @return valor ordinal
     */
    public int getOrdinal() {
        return ordinal;
    }

    /**
     * Obtenha o valor {@link Extension # plugins ()}, que foi atribuído à extensão.
     *
     * @return valor ordinal
     */
    public List<String> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    /**
     * Obtenha o valor {@link Extension # points ()}, que foi atribuído à extensão.
     *
     * @return valor ordinal
     */
    public List<String> getPoints() {
        return Collections.unmodifiableList(points);
    }

}
