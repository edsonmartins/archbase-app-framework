package br.com.archbase.plugin.manager.processor;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;


class ServiceProviderExtensionStorageTest {

    @Test
    void ensureServiceProviderExtensionStorageReadWorks() throws IOException {
        final StringReader file = new StringReader("#hello\n    World");
        final Set<String> entries = new HashSet<>();
        ServiceProviderExtensionStorage.read(file, entries);

        assertThat(entries.size(), is(1));
        assertThat(entries.contains("World"), is(true));
    }

    @Test
    void ensureReadingExtensionsProducesCorrectListOfExtensions() {
        final StringReader file = new StringReader("#hello\n    World");
        final ExtensionAnnotationProcessor processor = mock(ExtensionAnnotationProcessor.class);
        final Map<String, Set<String>> extensions = new HashMap<>();
        extensions.put("hello", Collections.singleton("world"));

        given(processor.getExtensions()).willReturn(extensions);
        ServiceProviderExtensionStorage extensionStorage = new ServiceProviderExtensionStorage(processor) {

            @Override
            protected Filer getFiler() {
                try {
                    Filer filer = mock(Filer.class);
                    FileObject fileObject = mock(FileObject.class);
                    given(fileObject.openReader(true)).willReturn(file);
                    given(filer.getResource(
                            any(StandardLocation.class),
                            any(String.class),
                            any(String.class)
                    )).willReturn(fileObject);
                    return filer;
                } catch (IOException ex) {
                    throw new IllegalStateException("Não deveria ter chegado aqui");
                }
            }

        };

        Map<String, Set<String>> read = extensionStorage.read();
        assertThat(read.containsKey("hello"), is(true));
        assertThat(read.get("hello"), is(Collections.singleton("World")));
    }

}
