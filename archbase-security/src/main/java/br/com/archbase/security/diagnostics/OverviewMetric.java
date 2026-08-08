package br.com.archbase.security.diagnostics;

/**
 * As métricas do panorama que têm detalhe navegável.
 *
 * <p>Enum, e não string livre, porque o valor chega pela URL: cada constante é uma consulta
 * conhecida, e o que não estiver aqui é 400 em vez de virar filtro arbitrário sobre o catálogo.
 *
 * <p>Cobre os números que apontam para algo a fazer. Contagens que são apenas denominador — total de
 * usuários, de recursos, de ações — não entram: a lista completa delas já existe nas telas de
 * cadastro, e repeti-la aqui seria uma segunda porta para o mesmo dado.
 */
public enum OverviewMetric {

    /** Concessões sobre ação ou recurso inativo — o acesso que o operador acredita ter concedido. */
    PERMISSIONS_POINTING_TO_INACTIVE,

    /** Usuários que passam direto pelo portão GRANT. */
    ADMINISTRATORS,

    /** Ações desativadas no catálogo. São a origem das permissões inertes. */
    ACTIONS_INACTIVE,

    /** Recursos de API desativados, normalmente pela varredura não achar {@code @HasPermission}. */
    API_RESOURCES_INACTIVE,

    /** Recursos sem nenhuma ação — telas ainda não abertas neste tenant. */
    RESOURCES_WITHOUT_ACTION,

    /** Ações que existem no catálogo e ninguém recebeu. */
    ACTIONS_WITHOUT_PERMISSION
}
