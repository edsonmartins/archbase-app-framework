package br.com.archbase.security.diagnostics;

/**
 * Um nó da árvore de objetos de segurança.
 *
 * <p><b>Forma uniforme, de propósito.</b> Pessoa, grupo, perfil, recurso e ação são coisas
 * diferentes, mas na árvore todas precisam da mesma coisa: um rótulo, um número à direita e a
 * indicação de que há algo errado ali dentro. Uma forma só significa um componente de árvore só —
 * em vez de cinco listas que divergem com o tempo.
 *
 * @param kind        a classe do objeto, que decide o ícone e para onde o clique leva
 * @param badge       o número à direita — membros do grupo, ações do recurso. {@code null} quando
 *                    não há contagem que ajude
 * @param hasChildren se o nó pode ser aberto. Calculado no servidor porque só ele sabe, e sem isso
 *                    a árvore mostraria seta de expandir em folha
 * @param severity    {@code null}, {@code "warning"} ou {@code "critical"} — o marcador que leva o
 *                    olho até onde há problema sem obrigar a abrir ramo por ramo
 */
public record TreeNode(
        String id,
        TreeNodeKind kind,
        String label,
        String badge,
        boolean hasChildren,
        String severity) {

    /** As classes de objeto que a árvore navega. */
    public enum TreeNodeKind {
        USER, GROUP, PROFILE, RESOURCE, ACTION
    }
}
