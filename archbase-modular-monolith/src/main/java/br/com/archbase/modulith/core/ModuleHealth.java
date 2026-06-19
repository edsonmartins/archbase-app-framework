package br.com.archbase.modulith.core;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

/**
 * Informações de saúde de um módulo.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@Getter
@ToString
@Builder
public class ModuleHealth {

    /**
     * Nome do módulo.
     */
    private final String moduleName;

    /**
     * Estado atual do módulo.
     */
    private final ModuleState state;

    /**
     * Indica se o módulo está saudável.
     */
    private final boolean healthy;

    /**
     * Mensagem descritiva do estado.
     */
    private final String message;

    /**
     * Timestamp da última verificação.
     */
    @Builder.Default
    private final Instant lastChecked = Instant.now();

    /**
     * Detalhes adicionais sobre a saúde.
     */
    private final Map<String, Object> details;

    /**
     * Cria uma instância indicando módulo saudável.
     */
    public static ModuleHealth healthy(String moduleName) {
        return ModuleHealth.builder()
                .moduleName(moduleName)
                .state(ModuleState.STARTED)
                .healthy(true)
                .message("Module is healthy")
                .build();
    }

    /**
     * Cria uma instância indicando módulo não saudável.
     */
    public static ModuleHealth unhealthy(String moduleName, String message) {
        return ModuleHealth.builder()
                .moduleName(moduleName)
                .state(ModuleState.FAILED)
                .healthy(false)
                .message(message)
                .build();
    }

    /**
     * Cria uma instância indicando módulo não encontrado.
     */
    public static ModuleHealth notFound(String moduleName) {
        return ModuleHealth.builder()
                .moduleName(moduleName)
                .state(ModuleState.CREATED)
                .healthy(false)
                .message("Module not found")
                .build();
    }
}
