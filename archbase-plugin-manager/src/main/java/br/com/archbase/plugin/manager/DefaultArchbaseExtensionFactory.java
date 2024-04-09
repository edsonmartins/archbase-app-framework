package br.com.archbase.plugin.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A implementação padrão para {@link ExtensionFactory}.
 * Ele usa o método {@link Class#getConstructor}.
 */
public class DefaultArchbaseExtensionFactory implements ExtensionFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultArchbaseExtensionFactory.class);

    /**
     * Cria uma instância de extensão.
     */
    @Override
    @SuppressWarnings("java:S1874")
    public <T> T create(Class<T> extensionClass) {
        log.debug("Criar instância para extensão '{}'", extensionClass.getName());
        try {
            return extensionClass.newInstance();
        } catch (Exception e) {
            throw new PluginRuntimeException(e);
        }
    }

}
