package br.com.archbase.plugin.manager.processor;

import br.com.archbase.plugin.manager.Extension;

import javax.annotation.processing.FilerException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Armazena {@link Extension} s em {@code META-INF/extensions.idx}.
 */
public class LegacyExtensionStorage extends ExtensionStorage {

    public static final String EXTENSIONS_RESOURCE = "META-INF/extensions.idx";

    public LegacyExtensionStorage(ExtensionAnnotationProcessor processor) {
        super(processor);
    }

    @Override
    @SuppressWarnings("java:S2147")
    public Map<String, Set<String>> read() {
        Map<String, Set<String>> extensions = new HashMap<>();

        try {
            FileObject file = getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", EXTENSIONS_RESOURCE);
            // TODO tente calcular o ponto de extensão
            Set<String> entries = new HashSet<>();
            read(file.openReader(true), entries);
            extensions.put(null, entries);
        } catch (FileNotFoundException | NoSuchFileException e) {
            // não existe, ignore
        } catch (FilerException e) {
            // reabrir o arquivo para leitura ou depois de escrever é ignorável
        } catch (IOException e) {
            error(e.getMessage());
        }

        return extensions;
    }

    @Override
    @SuppressWarnings("java:S2147")
    public void write(Map<String, Set<String>> extensions) {
        try {
            FileObject file = getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", EXTENSIONS_RESOURCE);
            try (BufferedWriter writer = new BufferedWriter(file.openWriter())) {
                writer.write("# Gerado por Archbase"); // write header
                writer.newLine();
                for (Map.Entry<String, Set<String>> entry : extensions.entrySet()) {
                    for (String extension : entry.getValue()) {
                        writer.write(extension);
                        writer.newLine();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // é a primeira vez, crie o arquivo
        } catch (FilerException e) {
            // reabrir o arquivo para leitura ou depois de escrever é ignorável
        } catch (IOException e) {
            error(e.toString());
        }
    }

}
