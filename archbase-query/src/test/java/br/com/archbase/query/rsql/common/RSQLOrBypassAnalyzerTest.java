package br.com.archbase.query.rsql.common;

import br.com.archbase.query.rsql.parser.RSQLParser;
import br.com.archbase.query.rsql.parser.ast.Node;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testa o analisador anti-bypass por OR: garante que seletores protegidos (ex.: {@code tenantId})
 * não possam ser anulados por uma expressão {@code OR} no filtro RSQL.
 */
class RSQLOrBypassAnalyzerTest {

    private static final Set<String> TENANT = Set.of("tenantId");

    private static Node parse(String rsql) {
        return new RSQLParser(RSQLOperators.supportedOperators()).parse(rsql);
    }

    private static void analyze(String rsql, Set<String> protectedSelectors) {
        RSQLOrBypassAnalyzer.assertNotBypassable(parse(rsql), protectedSelectors);
    }

    @Test
    void rejeitaBypassClassicoOrDeMenorPrecedencia() {
        // tenantId==X;name==a,name==b  =>  (tenantId AND name) OR name  -> vaza tenant
        assertThrows(IllegalArgumentException.class,
                () -> analyze("tenantId==X;name==a,name==b", TENANT));
    }

    @Test
    void rejeitaOrDiretoEnvolvendoSeletorProtegido() {
        assertThrows(IllegalArgumentException.class,
                () -> analyze("tenantId==X,name==b", TENANT));
    }

    @Test
    void rejeitaSeletorProtegidoAninhadoEmGrupoOr() {
        assertThrows(IllegalArgumentException.class,
                () -> analyze("name==a;(tenantId==X,name==b)", TENANT));
    }

    @Test
    void permiteAndPuroComSeletorProtegido() {
        assertDoesNotThrow(() -> analyze("tenantId==X;name==a", TENANT));
    }

    @Test
    void permiteOrEntreCamposNaoProtegidos() {
        assertDoesNotThrow(() -> analyze("name==a,name==b", TENANT));
        assertDoesNotThrow(() -> analyze("tenantId==X;(name==a,sku==b)", TENANT));
    }

    @Test
    void noOpQuandoNaoHaSeletoresProtegidos() {
        assertDoesNotThrow(() -> analyze("tenantId==X,name==b", Set.of()));
        assertDoesNotThrow(() -> RSQLOrBypassAnalyzer.assertNotBypassable(parse("tenantId==X,name==b"), null));
    }

    @Test
    void noOpQuandoAstNula() {
        assertDoesNotThrow(() -> RSQLOrBypassAnalyzer.assertNotBypassable(null, TENANT));
    }
}
