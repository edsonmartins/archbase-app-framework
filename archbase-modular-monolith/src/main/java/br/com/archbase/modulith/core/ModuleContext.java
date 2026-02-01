package br.com.archbase.modulith.core;

import br.com.archbase.modulith.communication.IntegrationEventBus;
import br.com.archbase.modulith.communication.ModuleGateway;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contexto de execução de um módulo.
 * <p>
 * Fornece acesso a recursos compartilhados e mecanismos de comunicação
 * entre módulos sem expor detalhes de implementação.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@Getter
@Builder
public class ModuleContext {

    /**
     * Descritor do módulo atual.
     */
    private final ModuleDescriptor moduleDescriptor;

    /**
     * Registro de módulos para consultas.
     */
    private final ModuleRegistry moduleRegistry;

    /**
     * Gateway para comunicação síncrona com outros módulos.
     */
    private final ModuleGateway moduleGateway;

    /**
     * Bus para publicação de eventos de integração.
     */
    private final IntegrationEventBus integrationEventBus;

    /**
     * Contexto Spring da aplicação.
     */
    private final ApplicationContext applicationContext;

    /**
     * Atributos customizados do módulo.
     */
    @Builder.Default
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Obtém um bean do contexto Spring.
     *
     * @param beanClass Classe do bean
     * @param <T>       Tipo do bean
     * @return Bean do contexto
     */
    public <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    /**
     * Obtém um bean do contexto Spring por nome.
     *
     * @param name      Nome do bean
     * @param beanClass Classe do bean
     * @param <T>       Tipo do bean
     * @return Bean do contexto
     */
    public <T> T getBean(String name, Class<T> beanClass) {
        return applicationContext.getBean(name, beanClass);
    }

    /**
     * Obtém um atributo customizado do módulo.
     *
     * @param key Chave do atributo
     * @param <T> Tipo do atributo
     * @return Valor do atributo ou empty se não existir
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key) {
        return Optional.ofNullable((T) attributes.get(key));
    }

    /**
     * Define um atributo customizado do módulo.
     *
     * @param key   Chave do atributo
     * @param value Valor do atributo
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Remove um atributo customizado do módulo.
     *
     * @param key Chave do atributo
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * Verifica se um módulo específico está disponível.
     *
     * @param moduleName Nome do módulo
     * @return true se o módulo está disponível e ativo
     */
    public boolean isModuleAvailable(String moduleName) {
        return moduleRegistry.getModule(moduleName)
                .map(m -> m.getState() == ModuleState.STARTED)
                .orElse(false);
    }

    /**
     * Obtém o nome do módulo atual.
     *
     * @return Nome do módulo
     */
    public String getModuleName() {
        return moduleDescriptor.getName();
    }

    /**
     * Obtém a versão do módulo atual.
     *
     * @return Versão do módulo
     */
    public String getModuleVersion() {
        return moduleDescriptor.getVersion();
    }
}
