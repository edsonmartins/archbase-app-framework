package br.com.archbase.maven.plugin.codegen.support.maker.values;

import static br.com.archbase.maven.plugin.codegen.support.maker.values.CommonValues.SPACE;


public enum ExpressionValues {

    EQUAL(SPACE + "=" + SPACE),
    AT("@");

    private String value;

    ExpressionValues(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
