package br.com.archbase.query.rsql.common;

import br.com.archbase.query.rsql.parser.ast.AndNode;
import br.com.archbase.query.rsql.parser.ast.ComparisonNode;
import br.com.archbase.query.rsql.parser.ast.ComparisonOperator;
import br.com.archbase.query.rsql.parser.ast.OrNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testa o controle de acesso por operador ({@link RSQLVisitorBase#operatorAccessControl}) e sua
 * configuração via {@link RSQLCommonSupport}. Sem regra declarada, todos os operadores são
 * permitidos (comportamento padrão, sem quebra).
 */
class RSQLOperatorAccessControlTest {

    /** Entidade fictícia usada apenas como chave de configuração. */
    static class Foo {
    }

    /** Subclasse mínima que expõe o método protegido para teste. */
    static class TestVisitor extends RSQLVisitorBase<Void, Void> {
        @Override
        public Map<String, String> getPropertyPathMapper() {
            return Collections.emptyMap();
        }

        @Override
        public Void visit(AndNode node, Void param) {
            return null;
        }

        @Override
        public Void visit(OrNode node, Void param) {
            return null;
        }

        @Override
        public Void visit(ComparisonNode node, Void param) {
            return null;
        }

        void check(Class<?> type, String name, ComparisonOperator op) {
            operatorAccessControl(type, name, op);
        }
    }

    private TestVisitor visitor;

    @BeforeEach
    void setUp() {
        RSQLCommonSupport.clear();
        // construir liga os mapas estáticos de RSQLVisitorBase aos de RSQLCommonSupport
        new RSQLCommonSupport();
        visitor = new TestVisitor();
    }

    @AfterEach
    void tearDown() {
        RSQLCommonSupport.clear();
    }

    @Test
    void semRegraPermiteQualquerOperador() {
        assertDoesNotThrow(() -> visitor.check(Foo.class, "email", RSQLOperators.LIKE));
        assertDoesNotThrow(() -> visitor.check(Foo.class, "email", RSQLOperators.EQUAL));
    }

    @Test
    void blacklistBloqueiaOperadorListado() {
        RSQLCommonSupport.addOperatorBlacklist(Foo.class, "email",
                RSQLOperators.LIKE, RSQLOperators.IGNORE_CASE_LIKE);

        assertThrows(IllegalArgumentException.class,
                () -> visitor.check(Foo.class, "email", RSQLOperators.LIKE));
        assertThrows(IllegalArgumentException.class,
                () -> visitor.check(Foo.class, "email", RSQLOperators.IGNORE_CASE_LIKE));
    }

    @Test
    void blacklistNaoAfetaOutrosOperadoresOuCampos() {
        RSQLCommonSupport.addOperatorBlacklist(Foo.class, "email", RSQLOperators.LIKE);

        assertDoesNotThrow(() -> visitor.check(Foo.class, "email", RSQLOperators.EQUAL));
        assertDoesNotThrow(() -> visitor.check(Foo.class, "name", RSQLOperators.LIKE));
    }

    @Test
    void whitelistPermiteApenasOperadoresListados() {
        RSQLCommonSupport.addOperatorWhitelist(Foo.class, "status", RSQLOperators.EQUAL);

        assertDoesNotThrow(() -> visitor.check(Foo.class, "status", RSQLOperators.EQUAL));
        assertThrows(IllegalArgumentException.class,
                () -> visitor.check(Foo.class, "status", RSQLOperators.LIKE));
    }

    @Test
    void whitelistNaoAfetaCamposSemRegra() {
        RSQLCommonSupport.addOperatorWhitelist(Foo.class, "status", RSQLOperators.EQUAL);

        assertDoesNotThrow(() -> visitor.check(Foo.class, "name", RSQLOperators.LIKE));
    }
}
