package br.com.archbase.modulith.core;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Descritor imutável contendo metadados de um módulo.
 * <p>
 * O ModuleDescriptor é criado durante a descoberta de módulos e contém
 * todas as informações necessárias para gerenciar o ciclo de vida do módulo.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@Getter
@ToString
@EqualsAndHashCode(of = "name")
@Builder
public class ModuleDescriptor {

    /**
     * Nome único do módulo.
     */
    private final String name;

    /**
     * Versão do módulo no formato semver.
     */
    private final String version;

    /**
     * Descrição do módulo.
     */
    private final String description;

    /**
     * Indica se o módulo está habilitado.
     */
    private final boolean enabled;

    /**
     * Ordem de inicialização do módulo.
     */
    private final int order;

    /**
     * Nomes dos módulos dos quais este depende.
     */
    @Builder.Default
    private final Set<String> dependencies = new HashSet<>();

    /**
     * Pacote base do módulo.
     */
    private final String basePackage;

    /**
     * Classe principal do módulo (anotada com @Module).
     */
    private final Class<?> moduleClass;

    /**
     * Instância do módulo (bean Spring).
     */
    private final Object moduleInstance;

    /**
     * Estado atual do módulo.
     */
    @Builder.Default
    private ModuleState state = ModuleState.CREATED;

    /**
     * Retorna as dependências como conjunto imutável.
     */
    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * Verifica se o módulo implementa ModuleLifecycle.
     */
    public boolean hasLifecycle() {
        return moduleInstance instanceof ModuleLifecycle;
    }

    /**
     * Retorna a instância como ModuleLifecycle, se aplicável.
     */
    public ModuleLifecycle getLifecycle() {
        if (hasLifecycle()) {
            return (ModuleLifecycle) moduleInstance;
        }
        return null;
    }

    /**
     * Atualiza o estado do módulo.
     */
    public void setState(ModuleState state) {
        this.state = state;
    }
}
