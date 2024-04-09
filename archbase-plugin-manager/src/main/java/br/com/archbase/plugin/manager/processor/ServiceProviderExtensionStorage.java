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
 * Armazena {@link Extension} s em {@code META-INF/services}.
 */
public class ServiceProviderExtensionStorage extends ExtensionStorage {

    public static final String EXTENSIONS_RESOURCE = "META-INF/services";

    public ServiceProviderExtensionStorage(ExtensionAnnotationProcessor processor) {
        super(processor);
    }

    @Override
    @SuppressWarnings("java:S2147")
    public Map<String, Set<String>> read() {
        Map<String, Set<String>> extensions = new HashMap<>();

        for (String extensionPoint : processor.getExtensions().keySet()) {
            try {
                FileObject file = getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", EXTENSIONS_RESOURCE
                        + "/" + extensionPoint);
                Set<String> entries = new HashSet<>();
                ExtensionStorage.read(file.openReader(true), entries);
                extensions.put(extensionPoint, entries);
            } catch (FileNotFoundException | NoSuchFileException e) {
                // não existe, ignore
            } catch (FilerException e) {
                // reabrir o arquivo para leitura ou depois de escrever é ignorável
            } catch (IOException e) {
                error(e.getMessage());
            }
        }

        return extensions;
    }

    @Override
    @SuppressWarnings("java:S2147")
    public void write(Map<String, Set<String>> extensions) {
        for (Map.Entry<String, Set<String>> entry : extensions.entrySet()) {
            String extensionPoint = entry.getKey();
            try {
                FileObject file = getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", EXTENSIONS_RESOURCE
                        + "/" + extensionPoint);
                try (BufferedWriter writer = new BufferedWriter(file.openWriter())) {
                    // escrever cabeçalho
                    writer.write("# Gerado por Archbase"); // write header
                    writer.newLine();
                    // escrever extensões
                    for (String extension : entry.getValue()) {
                        writer.write(extension);
                        if (!isExtensionOld(extensionPoint, extension)) {
                            writer.write(" # archbase extension");
                        }
                        writer.newLine();
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

    private boolean isExtensionOld(String extensionPoint, String extension) {
        return processor.getOldExtensions().containsKey(extensionPoint)
                && processor.getOldExtensions().get(extensionPoint).contains(extension);
    }

}
