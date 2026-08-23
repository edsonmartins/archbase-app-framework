package br.com.archbase.security.diagnostics;

/**
 * Os ramos que a árvore sabe abrir.
 *
 * <p>Enum, e não string livre, porque o valor chega pela URL: cada constante é uma consulta
 * conhecida e paginada. O que não estiver aqui responde 400, em vez de virar filtro arbitrário
 * sobre o catálogo de segurança.
 *
 * <p><b>Um ramo por consulta, e não uma árvore inteira.</b> Um tenant real tem 621 ações, 104
 * recursos e 2.229 concessões — mandar tudo numa carga só trava o navegador e obriga o servidor a
 * materializar o catálogo inteiro para exibir cinco linhas. Cada ramo busca ao abrir, com página e
 * filtro por nome.
 */
public enum TreeBranch {

    /** As pessoas do tenant. */
    USERS,

    /** Os grupos. É por aqui que a maior parte das concessões chega às pessoas. */
    GROUPS,

    /** Os perfis — a via mais ampla, vale para todo mundo que o tem. */
    PROFILES,

    /** Os recursos. Abrem em ações. */
    RESOURCES,

    /** As ações de um recurso. Exige {@code parentId}. */
    ACTIONS_OF_RESOURCE
}
