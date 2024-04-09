package br.com.archbase.maven.plugin.codegen.support.maker.values;

import static br.com.archbase.maven.plugin.codegen.support.maker.values.CommonValues.SPACE;


public enum ScopeValues {

    PUBLIC("public" + SPACE),
    PROTECTED("protected" + SPACE),
    PRIVATE("private" + SPACE);

    private String value;

    ScopeValues(String value) {
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
