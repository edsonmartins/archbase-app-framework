package br.com.archbase.query.rsql.parser.ast;


import java.util.regex.Pattern;

import static br.com.archbase.query.rsql.parser.ast.StringUtils.isBlank;


public final class ComparisonOperator {

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("=[a-zA-Z]*=|[><]=?|!=");

    private final String[] symbols;

    private final boolean multiValue;


    /**
     * @param symbols    Representação textual deste operador (e.g. <tt>=gt=</tt>); o primeiro item
     *                   é a representação primária, quaisquer outras são alternativas. Deve combinar
     *                   <tt>=[a-zA-Z]*=|[><]=?|!=</tt>.
     * @param multiValue Se este operador pode ser usado com vários argumentos. É então
     *                   validado em {@link NodesFactory}.
     * @throws IllegalArgumentException Se o {@code symbols} é também <tt>null</tt>, vazio,
     *                                  ou contêm símbolos ilegais.
     */
    public ComparisonOperator(String[] symbols, boolean multiValue) {
        Assert.notEmpty(symbols, "os símbolos não devem ser nulos ou vazios");
        for (String sym : symbols) {
            Assert.isTrue(isValidOperatorSymbol(sym), "símbolo deve combinar: %s", SYMBOL_PATTERN);
        }
        this.multiValue = multiValue;
        this.symbols = symbols.clone();
    }

    /**
     * @see #ComparisonOperator(String[], boolean)
     */
    public ComparisonOperator(String symbol, boolean multiValue) {
        this(new String[]{symbol}, multiValue);
    }

    /**
     * @see #ComparisonOperator(String[], boolean)
     */
    public ComparisonOperator(String symbol, String altSymbol, boolean multiValue) {
        this(new String[]{symbol, altSymbol}, multiValue);
    }

    /**
     * @see #ComparisonOperator(String[], boolean)
     */
    public ComparisonOperator(String... symbols) {
        this(symbols, false);
    }


    /**
     * Returns the primary representation of this operator.
     */
    public String getSymbol() {
        return symbols[0];
    }

    /**
     * Retorna todas as representações deste operador. O primeiro item é sempre o principal
     * representação.
     */
    public String[] getSymbols() {
        return symbols.clone();
    }

    /**
     * Se este operador pode ser usado com vários argumentos.
     */
    public boolean isMultiValue() {
        return multiValue;
    }


    /**
     * Se a string fornecida pode representar um operador.
     * Observação: os símbolos permitidos são limitados pela sintaxe RSQL (ou seja, analisador).
     */
    private boolean isValidOperatorSymbol(String str) {
        return !isBlank(str) && SYMBOL_PATTERN.matcher(str).matches();
    }


    @Override
    public String toString() {
        return getSymbol();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComparisonOperator)) return false;

        ComparisonOperator that = (ComparisonOperator) o;
        return getSymbol().equals(that.getSymbol());
    }

    @Override
    public int hashCode() {
        return getSymbol().hashCode();
    }
}
