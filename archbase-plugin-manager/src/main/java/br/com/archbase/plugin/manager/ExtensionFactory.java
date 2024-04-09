package br.com.archbase.plugin.manager;

/**
 * Cria uma instância de extensão.
 */
public interface ExtensionFactory {

    <T> T create(Class<T> extensionClass);

}
