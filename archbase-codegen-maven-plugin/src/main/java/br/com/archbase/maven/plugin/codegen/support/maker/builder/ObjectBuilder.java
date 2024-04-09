package br.com.archbase.maven.plugin.codegen.support.maker.builder;

import br.com.archbase.maven.plugin.codegen.support.maker.values.CommonValues;
import br.com.archbase.maven.plugin.codegen.util.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;


public class ObjectBuilder {

    private ObjectStructure objectStructure;
    private boolean attributeBottom;

    public ObjectBuilder(ObjectStructure objectStructure) {
        this.objectStructure = objectStructure;
    }

    public ObjectBuilder(ObjectStructure objectStructure, boolean attributeBottom) {
        this.objectStructure = objectStructure;
        this.attributeBottom = attributeBottom;
    }

    public ObjectBuilder setAttributeBottom(boolean attributeBottom) {
        this.attributeBottom = attributeBottom;
        return this;
    }

    private String buildComments() {
        SimpleDateFormat smf = new SimpleDateFormat("dd/MM/yyy");
        return new StringBuilder()
                .append(CommonValues.COMMENT_START.getValue())
                .append(CommonValues.COMMENT_BODY.getValue())
                .append(String.format("Gerado por %s em ", Constants.PROJECT_NAME))
                .append(smf.format(new Date()))
                .append(CommonValues.NEWLINE.getValue())
                .append(CommonValues.COMMENT_END.getValue())
                .toString();
    }

    public String build() {
        return new StringBuilder()
                .append(this.objectStructure.getObjectPackage())
                .append(CommonValues.NEWLINE.getValue())
                .append(this.objectStructure.getObjectImports())
                .append(this.buildComments())
                .append(this.objectStructure.getObjectAnnotations())
                .append(this.objectStructure.getObjectScope()).append(this.objectStructure.getObjectType()).append(this.objectStructure.getObjectName())
                .append(this.objectStructure.getObjectExtend())
                .append(this.objectStructure.getObjectImplements())
                .append(CommonValues.KEY_START.getValue())
                .append(!this.attributeBottom ? objectStructure.getObjectAttributes() : "")
                .append(this.objectStructure.getObjectConstructors())
                .append(this.objectStructure.getObjectMethods())
                .append(this.objectStructure.getObjectFunctions())
                .append(this.objectStructure.getObjectRawBody())
                .append(this.attributeBottom ? objectStructure.getObjectAttributes() : "")
                .append(CommonValues.NEWLINE.getValue())
                .append(CommonValues.KEY_END.getValue())
                .toString();
    }
}
