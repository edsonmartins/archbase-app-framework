package br.com.archbase.modulith.core;

/**
 * Estados possíveis de um módulo durante seu ciclo de vida.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public enum ModuleState {

    /**
     * Módulo foi criado mas ainda não inicializado.
     */
    CREATED,

    /**
     * Módulo está sendo inicializado.
     */
    STARTING,

    /**
     * Módulo está ativo e operacional.
     */
    STARTED,

    /**
     * Módulo está sendo parado.
     */
    STOPPING,

    /**
     * Módulo foi parado.
     */
    STOPPED,

    /**
     * Módulo falhou durante inicialização ou execução.
     */
    FAILED
}
