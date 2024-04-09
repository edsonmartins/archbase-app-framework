package br.com.archbase.query.rsql.parser;

/**
 * Esta exceção é lançada quando erros de análise são encontrados.
 * Você pode criar explicitamente objetos deste tipo de exceção por
 * chamando o método generateParseException no gerado
 * parser.
 * <p>
 * Você pode modificar esta classe para personalizar seu relatório de erros
 * mecanismos, desde que você retenha os campos públicos.
 */
@SuppressWarnings("all")
public class ParseException extends Exception {

    /**
     * O identificador de versão para esta classe serializável.
     * Incrementar apenas se a forma <i> serializada </i> do
     * mudanças de classe.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Este é o último token que foi consumido com sucesso. E se
     * este objeto foi criado devido a um erro de análise, o token
     * Seguindo este token será (portanto) o primeiro token de erro.
     */
    public Token currentToken;
    /**
     * Cada entrada nesta matriz é uma matriz de inteiros. Cada array
     * de inteiros representa uma sequência de tokens (por seu ordinal
     * valores) que é esperado neste ponto da análise.
     */
    public int[][] expectedTokenSequences;
    /**
     * Esta é uma referência à matriz "tokenImage" do
     * analisador no qual ocorreu o erro de análise. Esta matriz é
     * definido na interface gerada ... Constantes.
     */
    public String[] tokenImage;
    /**
     * A string de fim de linha para este SO.
     */
    protected String eol = System.getProperty("line.separator", "\n");

    /**
     * Este construtor é usado pelo método "generateParseException"
     * no analisador gerado. Chamar este construtor gera
     * um novo objeto deste tipo com os campos "currentToken",
     * Conjunto "expectTokenSequences" e "tokenImage".
     */
    public ParseException(Token currentTokenVal,
                          int[][] expectedTokenSequencesVal,
                          String[] tokenImageVal
    ) {
        super(initialise(currentTokenVal, expectedTokenSequencesVal, tokenImageVal));
        currentToken = currentTokenVal;
        expectedTokenSequences = expectedTokenSequencesVal;
        tokenImage = tokenImageVal;
    }

    /**
     * Os seguintes construtores são para uso por você para qualquer
     * propósito em que você pode pensar. Construindo a exceção desta
     * maneira faz com que a exceção se comporte da maneira normal - ou seja, como
     * documentado na classe "Throwable". Os campos "errorToken",
     * "expectedTokenSequences" e "tokenImage" não contêm
     * informação relevante. O código gerado JavaCC não usa
     * esses construtores.
     */

    public ParseException() {
        super();
    }

    /**
     * Construtor com mensagem.
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Ele usa "currentToken" e "expectTokenSequences" para gerar uma análise
     * mensagem de erro e a retorna. Se este objeto foi criado
     * devido a um erro de análise, e você não o detecta (ele é lançado
     * do analisador) a mensagem de erro correta
     * é exibido.
     */
    private static String initialise(Token currentToken,
                                     int[][] expectedTokenSequences,
                                     String[] tokenImage) {
        String eol = System.getProperty("line.separator", "\n");
        StringBuffer expected = new StringBuffer();
        int maxSize = 0;
        for (int i = 0; i < expectedTokenSequences.length; i++) {
            if (maxSize < expectedTokenSequences[i].length) {
                maxSize = expectedTokenSequences[i].length;
            }
            for (int j = 0; j < expectedTokenSequences[i].length; j++) {
                expected.append(tokenImage[expectedTokenSequences[i][j]]).append(' ');
            }
            if (expectedTokenSequences[i][expectedTokenSequences[i].length - 1] != 0) {
                expected.append("...");
            }
            expected.append(eol).append("    ");
        }
        String retval = "Encountered \"";
        Token tok = currentToken.next;
        for (int i = 0; i < maxSize; i++) {
            if (i != 0) retval += " ";
            if (tok.kind == 0) {
                retval += tokenImage[0];
                break;
            }
            retval += " " + tokenImage[tok.kind];
            retval += " \"";
            retval += add_escapes(tok.image);
            retval += " \"";
            tok = tok.next;
        }
        retval += "\" na linha " + currentToken.next.beginLine + ", coluna " + currentToken.next.beginColumn;
        retval += "." + eol;
        if (expectedTokenSequences.length == 1) {
            retval += "Estava esperando:" + eol + "    ";
        } else {
            retval += "Estava esperando um de:" + eol + "    ";
        }
        retval += expected.toString();
        return retval;
    }

    /**
     * Usado para converter caracteres brutos em sua versão de escape
     * quando esta versão bruta não pode ser usada como parte de um ASCII
     * literal de string.
     */
    static String add_escapes(String str) {
        StringBuffer retval = new StringBuffer();
        char ch;
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case 0:
                    continue;
                case '\b':
                    retval.append("\\b");
                    continue;
                case '\t':
                    retval.append("\\t");
                    continue;
                case '\n':
                    retval.append("\\n");
                    continue;
                case '\f':
                    retval.append("\\f");
                    continue;
                case '\r':
                    retval.append("\\r");
                    continue;
                case '\"':
                    retval.append("\\\"");
                    continue;
                case '\'':
                    retval.append("\\\'");
                    continue;
                case '\\':
                    retval.append("\\\\");
                    continue;
                default:
                    if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
                        String s = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u" + s.substring(s.length() - 4, s.length()));
                    } else {
                        retval.append(ch);
                    }
                    continue;
            }
        }
        return retval.toString();
    }

}

