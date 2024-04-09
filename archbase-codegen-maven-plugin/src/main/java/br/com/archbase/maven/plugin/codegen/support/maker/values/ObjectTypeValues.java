package br.com.archbase.maven.plugin.codegen.support.maker.values;

import static br.com.archbase.maven.plugin.codegen.support.maker.values.CommonValues.SPACE;


public enum ObjectTypeValues {

    CLASS("class" + SPACE),
    INTERFACE("interface" + SPACE);

    private String value;

    ObjectTypeValues(String value) {
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
