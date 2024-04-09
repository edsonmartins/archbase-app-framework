package br.com.archbase.maven.plugin.codegen.support.maker.values;

import static br.com.archbase.maven.plugin.codegen.support.maker.values.CommonValues.SPACE;


public enum ObjectValues {
    PACKAGE("package" + SPACE),
    IMPORT("import" + SPACE),
    IMPLEMENTS("implements" + SPACE),
    THIS("this."),
    SUPER("super("),
    EXTENDS("extends" + SPACE),
    RETURN("return" + SPACE);

    private String value;

    ObjectValues(String value) {
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
