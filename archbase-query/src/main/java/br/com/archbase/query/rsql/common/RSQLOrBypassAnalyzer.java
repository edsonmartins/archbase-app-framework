package br.com.archbase.query.rsql.common;

import br.com.archbase.query.rsql.parser.ast.ComparisonNode;
import br.com.archbase.query.rsql.parser.ast.LogicalNode;
import br.com.archbase.query.rsql.parser.ast.Node;
import br.com.archbase.query.rsql.parser.ast.OrNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Protege filtros obrigatórios (tenant, autorização) contra anulação via {@code OR}.
 *
 * <p>Em RSQL o operador {@code ,} (OR) tem precedência menor que {@code ;} (AND). Assim, um filtro
 * montado por concatenação como {@code tenantId==X;nome==a,nome==b} é interpretado como
 * {@code (tenantId==X AND nome==a) OR nome==b} — e o ramo direito do {@code OR} escapa do filtro de
 * tenant. Este analisador percorre a AST e rejeita a consulta quando um seletor protegido aparece
 * dentro de qualquer subárvore {@code OR}, fechando esse vetor de bypass.
 *
 * <p>Comportamento conservador e não-intrusivo: quando nenhum seletor protegido é informado para a
 * entidade, a análise é um no-op. A forma à prova de falhas continua sendo aplicar os filtros
 * obrigatórios como uma {@code Specification} combinada com {@code AND} separadamente do RSQL do
 * cliente; este analisador é defesa em profundidade contra injeção acidental por concatenação.
 *
 * @see RSQLCommonSupport#addProtectedSelector(Class, String...)
 */
@Slf4j
public final class RSQLOrBypassAnalyzer {

    private RSQLOrBypassAnalyzer() {
    }

    /**
     * Valida que nenhum seletor protegido pode ser anulado por um {@code OR} na consulta.
     *
     * @param node               raiz da AST RSQL (pode ser {@code null})
     * @param protectedSelectors seletores que não podem aparecer sob um {@code OR} (pode ser {@code null}/vazio)
     * @throws IllegalArgumentException quando um seletor protegido é encontrado dentro de um {@code OR}
     */
    public static void assertNotBypassable(Node node, Set<String> protectedSelectors) {
        if (node == null || protectedSelectors == null || protectedSelectors.isEmpty()) {
            return;
        }
        check(node, protectedSelectors, false);
    }

    private static void check(Node node, Set<String> protectedSelectors, boolean underOr) {
        if (node instanceof ComparisonNode) {
            ComparisonNode comparison = (ComparisonNode) node;
            if (underOr && protectedSelectors.contains(comparison.getSelector())) {
                log.warn("Tentativa de bypass de filtro obrigatório: seletor protegido [{}] sob OR", comparison.getSelector());
                throw new IllegalArgumentException(
                        "O seletor protegido '" + comparison.getSelector()
                                + "' não pode aparecer dentro de uma expressão OR (risco de bypass de filtro obrigatório)");
            }
        } else if (node instanceof OrNode) {
            for (Node child : ((OrNode) node).getChildren()) {
                check(child, protectedSelectors, true);
            }
        } else if (node instanceof LogicalNode) {
            // AndNode e quaisquer outros nós lógicos: preserva o contexto atual de OR.
            for (Node child : ((LogicalNode) node).getChildren()) {
                check(child, protectedSelectors, underOr);
            }
        }
    }
}
