package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.processor.LegacyExtensionStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


class LegacyExtensionStorageTest {

    /**
     * Teste para {@link LegacyExtensionStorage#read(Reader, Set)}.
     */
    @Test
    void testRead() throws IOException {
        Reader reader = new StringReader(
                "# comment\n"
                        + "org.archbase.demo.hello.HelloPlugin$HelloGreeting\n"
                        + "org.archbase.demo.welcome.WelcomePlugin$WelcomeGreeting\n"
                        + "org.archbase.demo.welcome.OtherGreeting\n");

        Set<String> entries = new HashSet<>();
        LegacyExtensionStorage.read(reader, entries);
        assertEquals(3, entries.size());
    }

}
