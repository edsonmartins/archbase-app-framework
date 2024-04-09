package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.FailTestExtension;
import br.com.archbase.plugin.manager.plugin.TestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


class DefaultArchbaseExtensionFactoryTest {

    private ExtensionFactory extensionFactory;

    @BeforeEach
    void setUp() {
        extensionFactory = new DefaultArchbaseExtensionFactory();
    }

    @AfterEach
    void tearDown() {
        extensionFactory = null;
    }

    /**
     * Teste do método de criação, da classe DefaultArchbaseExtensionFactory.
     */
    @Test
    void testCreate() {
        assertNotNull(extensionFactory.create(TestExtension.class));
    }

    /**
     * Teste do método de criação, da classe DefaultArchbaseExtensionFactory.
     */
    @Test
    void testCreateFailConstructor() {
        assertThrows(PluginRuntimeException.class, () -> extensionFactory.create(FailTestExtension.class));
    }

}
