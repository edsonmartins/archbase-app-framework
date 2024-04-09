package br.com.archbase.query.rsql.parser;

import br.com.archbase.query.rsql.parser.ast.ComparisonNode;
import br.com.archbase.query.rsql.parser.ast.LogicalOperator;
import br.com.archbase.query.rsql.parser.ast.Node;
import br.com.archbase.query.rsql.parser.ast.NodesFactory;
import org.apache.commons.text.StringEscapeUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("all")
final class Parser implements ParserConstants {

    static private int[] jj_la1_0;

    static {
        jj_la1_init_0();
    }

    final private int[] jj_la1 = new int[8];
    /**
     * Generated Token Manager.
     */
    public ParserTokenManager token_source;
    /**
     * Current token.
     */
    public Token token;
    /**
     * Next token.
     */
    public Token jj_nt;
    SimpleCharStream jj_input_stream;
    private NodesFactory factory;
    private int jj_ntk;
    private int jj_gen;
    private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
    private int[] jj_expentry;
    private int jj_kind = -1;

    public Parser(InputStream stream, String encoding, NodesFactory factory) {
        this(stream, encoding);
        this.factory = factory;
    }
    /**
     * Constructor com InputStream.
     */
    public Parser(java.io.InputStream stream) {
        this(stream, null);
    }
    /**
     * Constructor com InputStream e codificação fornecida
     */
    public Parser(java.io.InputStream stream, String encoding) {
        try {
            jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        token_source = new ParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 8; i++)
            jj_la1[i] = -1;
    }
    /**
     * Constructor.
     */
    public Parser(java.io.Reader stream) {
        jj_input_stream = new SimpleCharStream(stream, 1, 1);
        token_source = new ParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 8; i++)
            jj_la1[i] = -1;
    }
    /**
     * Constructor com Token Manager gerado.
     */
    public Parser(ParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 8; i++)
            jj_la1[i] = -1;
    }

    private static void jj_la1_init_0() {
        jj_la1_0 = new int[]{0x200, 0x100, 0x420, 0x3000, 0x4e0, 0x200, 0xc0, 0xe0,};
    }

    final public Node Input() throws ParseException {
        final Node node;
        node = Or();
        jj_consume_token(0);
        {
            if (true)
                return node;
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public Node Or() throws ParseException {
        final List<Node> nodes = new ArrayList<Node>(3);
        Node node;
        node = And();
        nodes.add(node);
        label_1:
        while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case OR:
                    ;
                    break;
                default:
                    jj_la1[0] = jj_gen;
                    break label_1;
            }
            jj_consume_token(OR);
            node = And();
            nodes.add(node);
        }
        {
            if (true)
                return nodes.size() != 1 ? factory.createLogicalNode(LogicalOperator.OR, nodes) : nodes.get(0);
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public Node And() throws ParseException {
        final List<Node> nodes = new ArrayList<Node>(3);
        Node node;
        node = Constraint();
        nodes.add(node);
        label_2:
        while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case AND:
                    ;
                    break;
                default:
                    jj_la1[1] = jj_gen;
                    break label_2;
            }
            jj_consume_token(AND);
            node = Constraint();
            nodes.add(node);
        }
        {
            if (true)
                return nodes.size() != 1 ? factory.createLogicalNode(LogicalOperator.AND, nodes) : nodes.get(0);
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public Node Constraint() throws ParseException {
        final Node node;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case LPAREN:
                node = Group();
                break;
            case UNRESERVED_STR:
                node = Comparison();
                break;
            default:
                jj_la1[2] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        {
            if (true)
                return node;
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public Node Group() throws ParseException {
        final Node node;
        jj_consume_token(LPAREN);
        node = Or();
        jj_consume_token(RPAREN);
        {
            if (true)
                return node;
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public ComparisonNode Comparison() throws ParseException {
        final String sel;
        final String op;
        final List<String> args;
        sel = Selector();
        op = Operator();
        args = Arguments();
        {
            if (true)
                return factory.createComparisonNode(op, sel, args);
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public String Selector() throws ParseException {
        token = jj_consume_token(UNRESERVED_STR);
        {
            if (true)
                return token.image;
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public String Operator() throws ParseException {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case COMP_FIQL:
                token = jj_consume_token(COMP_FIQL);
                break;
            case COMP_ALT:
                token = jj_consume_token(COMP_ALT);
                break;
            default:
                jj_la1[3] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        {
            if (true)
                return token.image;
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public List<String> Arguments() throws ParseException {
        final Object value;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case LPAREN:
                jj_consume_token(LPAREN);
                value = CommaSepArguments();
                jj_consume_token(RPAREN);
            {
                if (true)
                    return (List) value;
            }
            break;
            case UNRESERVED_STR:
            case SINGLE_QUOTED_STR:
            case DOUBLE_QUOTED_STR:
                value = Argument();
            {
                if (true)
                    return Arrays.asList((String) value);
            }
            break;
            default:
                jj_la1[4] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public List<String> CommaSepArguments() throws ParseException {
        final List<String> list = new ArrayList<String>(3);
        String arg;
        arg = Argument();
        list.add(arg);
        label_3:
        while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case OR:
                    ;
                    break;
                default:
                    jj_la1[5] = jj_gen;
                    break label_3;
            }
            jj_consume_token(OR);
            arg = Argument();
            list.add(arg);
        }
        {
            if (true)
                return list;
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    final public String Argument() throws ParseException {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case UNRESERVED_STR:
                token = jj_consume_token(UNRESERVED_STR);
            {
                if (true)
                    return token.image;
            }
            break;
            case SINGLE_QUOTED_STR:
            case DOUBLE_QUOTED_STR:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                    case DOUBLE_QUOTED_STR:
                        token = jj_consume_token(DOUBLE_QUOTED_STR);
                        break;
                    case SINGLE_QUOTED_STR:
                        token = jj_consume_token(SINGLE_QUOTED_STR);
                        break;
                    default:
                        jj_la1[6] = jj_gen;
                        jj_consume_token(-1);
                        throw new ParseException();
                }
            {
                if (true)
                    return StringEscapeUtils.unescapeJava(token.image.substring(1, token.image.length() - 1));
            }
            break;
            default:
                jj_la1[7] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        throw new Error("Declaração de retorno ausente na função");
    }

    /**
     * Reinitialise.
     */
    public void ReInit(java.io.InputStream stream) {
        ReInit(stream, null);
    }

    /**
     * Reinitialise.
     */
    public void ReInit(java.io.InputStream stream, String encoding) {
        try {
            jj_input_stream.ReInit(stream, encoding, 1, 1);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 8; i++)
            jj_la1[i] = -1;
    }

    /**
     * Reinitialise.
     */
    public void ReInit(java.io.Reader stream) {
        jj_input_stream.ReInit(stream, 1, 1);
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 8; i++)
            jj_la1[i] = -1;
    }

    /**
     * Reinitialise.
     */
    public void ReInit(ParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 8; i++)
            jj_la1[i] = -1;
    }

    private Token jj_consume_token(int kind) throws ParseException {
        Token oldToken;
        if ((oldToken = token).next != null)
            token = token.next;
        else
            token = token.next = token_source.getNextToken();
        jj_ntk = -1;
        if (token.kind == kind) {
            jj_gen++;
            return token;
        }
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();
    }

    /**
     * Pegue o próximo token.
     */
    final public Token getNextToken() {
        if (token.next != null)
            token = token.next;
        else
            token = token.next = token_source.getNextToken();
        jj_ntk = -1;
        jj_gen++;
        return token;
    }

    /**
     * Obtenha o token específico.
     */
    final public Token getToken(int index) {
        Token t = token;
        for (int i = 0; i < index; i++) {
            if (t.next != null)
                t = t.next;
            else
                t = t.next = token_source.getNextToken();
        }
        return t;
    }

    private int jj_ntk() {
        if ((jj_nt = token.next) == null)
            return (jj_ntk = (token.next = token_source.getNextToken()).kind);
        else
            return (jj_ntk = jj_nt.kind);
    }

    /**
     * Gerar ParseException.
     */
    public ParseException generateParseException() {
        jj_expentries.clear();
        boolean[] la1tokens = new boolean[14];
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 8; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 14; i++) {
            if (la1tokens[i]) {
                jj_expentry = new int[1];
                jj_expentry[0] = i;
                jj_expentries.add(jj_expentry);
            }
        }
        int[][] exptokseq = new int[jj_expentries.size()][];
        for (int i = 0; i < jj_expentries.size(); i++) {
            exptokseq[i] = jj_expentries.get(i);
        }
        return new ParseException(token, exptokseq, tokenImage);
    }

    /**
     * Habilitar rastreamento.
     */
    final public void enable_tracing() {
    }

    /**
     * Desativar rastreamento.
     */
    final public void disable_tracing() {
    }

}
