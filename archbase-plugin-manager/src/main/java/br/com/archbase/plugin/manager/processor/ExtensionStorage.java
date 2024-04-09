package br.com.archbase.plugin.manager.processor;

import br.com.archbase.plugin.manager.Extension;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * É um armazenamento (banco de dados) que persiste {@link Extension} s.
 * As operações padrão suportadas pelo armazenamento são {@link #read} e {@link #write}.
 * O armazenamento é preenchido por {@link ExtensionAnnotationProcessor}.
 */
public abstract class ExtensionStorage {

    private static final Pattern COMMENT = Pattern.compile("#.*");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    protected final ExtensionAnnotationProcessor processor;

    protected ExtensionStorage(ExtensionAnnotationProcessor processor) {
        this.processor = processor;
    }

    public static void read(Reader reader, Set<String> entries) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = COMMENT.matcher(line).replaceFirst("");
                line = WHITESPACE.matcher(line).replaceAll("");
                if (line.length() > 0) {
                    entries.add(line);
                }
            }
        }
    }

    public abstract Map<String, Set<String>> read();

    public abstract void write(Map<String, Set<String>> extensions);

    /**
     * Método auxiliar.
     */
    protected Filer getFiler() {
        return processor.getProcessingEnvironment().getFiler();
    }

    /**
     * Método auxiliar.
     */
    protected void error(String message, Object... args) {
        processor.error(message, args);
    }

    /**
     * Método auxiliar.
     */
    protected void error(Element element, String message, Object... args) {
        processor.error(element, message, args);
    }

    /**
     * Método auxiliar.
     */
    protected void info(String message, Object... args) {
        processor.info(message, args);
    }

    /**
     * Método auxiliar.
     */
    protected void info(Element element, String message, Object... args) {
        processor.info(element, message, args);
    }

}
