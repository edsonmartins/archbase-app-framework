package br.com.archbase.security.diagnostics;

import java.util.Map;

/**
 * O estado da segurança do tenant em números.
 *
 * <p>Cada campo existe para responder a uma pergunta que, sem ele, só se responde abrindo o banco.
 *
 * @param permissionsPointingToInactive concessões sobre ação ou recurso inativo — as que a tela já
 *                                      ignora e o {@code @HasPermission} ainda honra
 * @param permissionsBySecurityType     quantas concessões foram para usuário, grupo e perfil
 * @param apiResourcesInactive          recursos de API desativados. Quando a varredura roda sem
 *                                      encontrar {@code @HasPermission} correspondente, é ela que
 *                                      os desativa — e um número alto aqui costuma ser esse caso
 * @param resourcesWithoutAction        recursos sem nenhuma ação. Não é defeito: o frontend
 *                                      registra a tela na primeira renderização
 * @param actionsWithoutPermission      ações no catálogo que ninguém recebeu
 */
public record AccessOverview(
        long users,
        long administrators,
        long groups,
        long profiles,
        long resources,
        long apiResources,
        long apiResourcesInactive,
        long resourcesWithoutAction,
        long actions,
        long actionsInactive,
        long actionsWithoutPermission,
        long permissions,
        long permissionsPointingToInactive,
        Map<String, Long> permissionsBySecurityType,
        Flags flags) {

    public AccessOverview {
        permissionsBySecurityType = permissionsBySecurityType == null
                ? Map.of() : Map.copyOf(permissionsBySecurityType);
    }

    /**
     * O estado das proteções que mudam comportamento.
     *
     * <p>Vai junto de propósito: um número só é interpretável sabendo quais portões estão ligados.
     * As 1.279 concessões inertes do gestor-rq são inofensivas enquanto nenhum endpoint consulta o
     * catálogo, e viram acesso indevido no dia em que consultarem.
     */
    public record Flags(
            boolean requireActive,
            boolean scanConfigured,
            String adminEndpointsPolicy,
            String requireRoleNoResolverPolicy,
            boolean roleResolverRegistered) {
    }
}
