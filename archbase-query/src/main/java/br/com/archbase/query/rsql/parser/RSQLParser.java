package br.com.archbase.query.rsql.parser;

import br.com.archbase.query.rsql.parser.ast.ComparisonOperator;
import br.com.archbase.query.rsql.parser.ast.Node;
import br.com.archbase.query.rsql.parser.ast.NodesFactory;
import br.com.archbase.query.rsql.parser.ast.RSQLOperators;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * Analisador do RSQL (RESTful Service Query Language).
 *
 * <p>
 * RSQL é uma linguagem de consulta para filtragem parametrizada de entradas em RESTful
 * APIs. É um superconjunto do <a href =
 * "http://tools.ietf.org/html/draft-nottingham-atompub-fiql-00">FIQL </a> (Feed
 * Item Query Language), portanto, também pode ser usado para analisar FIQL.
 * </p>
 *
 * <p>
 * <b>Grammar in EBNF notation:</b>
 *
 * <pre>
 * {@code
 * input          = or, EOF;
 * or             = and, { ( "," | " or " ) , and };
 * and            = constraint, { ( ";" | " and " ), constraint };
 * constraint     = ( group | comparison );
 * group          = "(", or, ")";
 *
 * comparison     = selector, comparator, arguments;
 * selector       = unreserved-str;
 *
 * comparator     = comp-fiql | comp-alt;
 * comp-fiql      = ( ( "=", { ALPHA } ) | "!" ), "=";
 * comp-alt       = ( ">" | "<" ), [ "=" ];
 *
 * arguments      = ( "(", value, { "," , value }, ")" ) | value;
 * value          = unreserved-str | double-quoted | single-quoted;
 *
 * unreserved-str = unreserved, { unreserved }
 * single-quoted  = "'", { ( escaped | all-chars - ( "'" | "\" ) ) }, "'";
 * double-quoted  = '"', { ( escaped | all-chars - ( '"' | "\" ) ) }, '"';
 *
 * reserved       = '"' | "'" | "(" | ")" | ";" | "," | "=" | "!" | "~" | "<" | ">" | " ";
 * unreserved     = all-chars - reserved;
 * escaped        = "\", all-chars;
 * all-chars      = ? all unicode characters ?;
 * }
 * </pre>
 */

@SuppressWarnings("all")
public final class RSQLParser {

    private static final Charset ENCODING = Charset.forName("UTF-8");

    private final NodesFactory nodesFactory;

    /**
     * Cria uma nova instância de {@code RSQLParser} com o conjunto padrão de
     * operadores de comparação.
     */
    public RSQLParser() {
        this.nodesFactory = new NodesFactory(RSQLOperators.defaultOperators());
    }

    /**
     * Cria uma nova instância de {@code RSQLParser} que suporta apenas o especificado
     * operadores de comparação.
     *
     * @param operators Um conjunto de operadores de comparação com suporte. Não deve ser
     *                  <tt>null</tt> ou vazio.
     */
    public RSQLParser(Set<ComparisonOperator> operators) {
        if (operators == null || operators.isEmpty()) {
            throw new IllegalArgumentException("operadores não devem ser nulos ou vazios");
        }
        this.nodesFactory = new NodesFactory(operators);
    }

    public static void main(String[] args) {
        Node rootNode = new RSQLParser().parse("((firstName==john;lastName==doe),(firstName==aaron;lastName==carter));((age==21;height==90),(age==30;height==100))");
        System.out.println(rootNode);
    }

    /**
     * Analisa a expressão RSQL e retorna AST.
     *
     * @param query A expressão de consulta a ser analisada.
     * @return Uma raiz do AST analisado.
     * @throws RSQLParserException      Se alguma exceção ocorreu durante a análise,
     *                                  ou seja, a {@code query} é sintaticamente
     *                                  invalida.
     * @throws IllegalArgumentException Se a {@code query} for <tt> null </tt>.
     */
    public Node parse(String query) throws RSQLParserException {
        if (query == null) {
            throw new IllegalArgumentException("a consulta não deve ser nula");
        }
        InputStream is = new ByteArrayInputStream(query.getBytes(ENCODING));
        Parser parser = new Parser(is, ENCODING.name(), nodesFactory);

        try {
            return parser.Input();

        } catch (Exception ex) {
            throw new RSQLParserException(ex);
        }
    }
}
