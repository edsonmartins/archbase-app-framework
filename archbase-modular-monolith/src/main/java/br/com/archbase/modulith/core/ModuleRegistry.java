package br.com.archbase.modulith.core;

import java.util.List;
import java.util.Optional;

/**
 * Registro central de módulos da aplicação.
 * <p>
 * O ModuleRegistry é responsável por:
 * <ul>
 *   <li>Registrar e descobrir módulos</li>
 *   <li>Validar dependências entre módulos</li>
 *   <li>Gerenciar o ciclo de vida dos módulos</li>
 *   <li>Fornecer informações de saúde dos módulos</li>
 * </ul>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public interface ModuleRegistry {

    /**
     * Registra um módulo no registro.
     *
     * @param module Descritor do módulo
     * @throws ModuleRegistrationException se o módulo já estiver registrado
     */
    void register(ModuleDescriptor module);

    /**
     * Remove um módulo do registro.
     *
     * @param moduleName Nome do módulo
     * @return true se o módulo foi removido
     */
    boolean unregister(String moduleName);

    /**
     * Obtém um módulo pelo nome.
     *
     * @param name Nome do módulo
     * @return Optional com o descritor do módulo
     */
    Optional<ModuleDescriptor> getModule(String name);

    /**
     * Obtém todos os módulos registrados.
     *
     * @return Lista de descritores de módulos
     */
    List<ModuleDescriptor> getAllModules();

    /**
     * Obtém os módulos habilitados.
     *
     * @return Lista de módulos habilitados
     */
    List<ModuleDescriptor> getEnabledModules();

    /**
     * Obtém as dependências diretas de um módulo.
     *
     * @param moduleName Nome do módulo
     * @return Lista de dependências
     */
    List<ModuleDescriptor> getDependencies(String moduleName);

    /**
     * Obtém os módulos que dependem de um módulo específico.
     *
     * @param moduleName Nome do módulo
     * @return Lista de módulos dependentes
     */
    List<ModuleDescriptor> getDependents(String moduleName);

    /**
     * Valida as dependências de todos os módulos.
     * <p>
     * Verifica se todas as dependências declaradas existem e
     * se não há ciclos de dependência.
     *
     * @throws ModuleDependencyException se houver problemas nas dependências
     */
    void validateDependencies();

    /**
     * Obtém a saúde de um módulo específico.
     *
     * @param moduleName Nome do módulo
     * @return Informações de saúde do módulo
     */
    ModuleHealth getHealth(String moduleName);

    /**
     * Obtém a saúde de todos os módulos.
     *
     * @return Lista de informações de saúde
     */
    List<ModuleHealth> getAllHealth();

    /**
     * Verifica se um módulo está registrado.
     *
     * @param moduleName Nome do módulo
     * @return true se o módulo está registrado
     */
    boolean isRegistered(String moduleName);

    /**
     * Obtém os módulos na ordem de inicialização.
     * <p>
     * Considera dependências e ordem configurada.
     *
     * @return Lista ordenada de módulos
     */
    List<ModuleDescriptor> getStartupOrder();

    /**
     * Obtém os módulos na ordem de encerramento (inverso da inicialização).
     *
     * @return Lista ordenada de módulos
     */
    List<ModuleDescriptor> getShutdownOrder();
}
