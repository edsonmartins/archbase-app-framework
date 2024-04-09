package br.com.archbase.maven.plugin.codegen.plugin;

import br.com.archbase.maven.plugin.codegen.util.ArchbaseDataLogger;
import br.com.archbase.maven.plugin.codegen.util.ArchbaseDataMojoException;
import br.com.archbase.maven.plugin.codegen.util.Constants;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.LinkedHashSet;
import java.util.Set;


public abstract class CommonsMojo extends AbstractMojo {

    @Parameter(name = Constants.ENTITY_PACKAGE)
    protected String[] entityPackage;

    @Parameter(name = Constants.REPOSITORY_PACKAGE)
    protected String repositoryPackage;

    @Parameter(name = Constants.REPOSITORY_POSTFIX, defaultValue = "Repository")
    protected String repositoryPostfix;

    @Parameter(name = Constants.SERVICE_PACKAGE)
    protected String servicePackage;

    @Parameter(name = Constants.SERVICE_POSTFIX, defaultValue = "Service")
    protected String servicePostfix;

    @Parameter(name = Constants.RESOURCE_PACKAGE)
    protected String resourcePackage;

    @Parameter(name = Constants.RESOURCE_POSTFIX, defaultValue = "Resource")
    protected String resourcePostfix;

    @Parameter(name = Constants.DTO_PACKAGE)
    protected String dtoPackage;

    @Parameter(name = Constants.DTO_POSTFIX, defaultValue = "DTO")
    protected String dtoPostfix;

    @Parameter(name = Constants.ONLY_ANNOTATIONS, defaultValue = "false")
    protected Boolean onlyAnnotations;

    @Parameter(name = Constants.OVERWRITE, defaultValue = "false")
    protected Boolean overwrite;

    @Parameter(name = Constants.API_VERSION, defaultValue = "1")
    protected String apiVersion;

    @Parameter(name = Constants.REVISION_NUMBER_CLASS, defaultValue = "java.lang.Integer")
    protected String revisionNumberClass;

    @Parameter(name = Constants.EXTENDS)
    protected String[] additionalExtends;

    @Component
    protected MavenProject project;

    protected Set<String> additionalExtendsList = new LinkedHashSet<>();

    @SuppressWarnings("java:S3776")
    public void validateField(String parameter) throws ArchbaseDataMojoException {

        boolean errorFound = Boolean.FALSE;

        switch (parameter) {
            case Constants.ENTITY_PACKAGE:
                if (entityPackage == null) {
                    errorFound = Boolean.TRUE;
                }
                break;
            case Constants.REPOSITORY_PACKAGE:
                if (repositoryPackage == null) {
                    errorFound = Boolean.TRUE;
                }
                break;
            case Constants.REPOSITORY_POSTFIX:
                if (repositoryPostfix == null) {
                    errorFound = Boolean.TRUE;
                }
                break;
            case Constants.DTO_POSTFIX:
                if (dtoPostfix == null) {
                    errorFound = Boolean.TRUE;
                }
                break;
            case Constants.SERVICE_PACKAGE:
                if (servicePackage == null) {
                    errorFound = Boolean.TRUE;
                }
                break;
            case Constants.SERVICE_POSTFIX:
                if (servicePostfix == null) {
                    errorFound = Boolean.TRUE;
                }
                break;
            case Constants.RESOURCE_PACKAGE:
                if (resourcePackage == null) {
                    errorFound = Boolean.TRUE;
                }
                break;
            case Constants.RESOURCE_POSTFIX:
                if (resourcePostfix == null) {
                    errorFound = Boolean.TRUE;
                }
                break;
            case Constants.EXTENDS:
                if (additionalExtends != null) {
                    this.validateExtends();
                }
                break;
            default:
                ArchbaseDataLogger.addError(String.format("%s parâmetro de configuração não encontrado!", parameter));
                throw new ArchbaseDataMojoException();
        }

        if (errorFound) {
            ArchbaseDataLogger.addError(String.format("%s configuração não encontrada!", parameter));
            throw new ArchbaseDataMojoException();
        }
    }

    private void validateExtends() throws ArchbaseDataMojoException {
        String extendTemporal;
        boolean errorValidate = Boolean.FALSE;
        for (int i = 0; i < additionalExtends.length; i++) {
            extendTemporal = additionalExtends[i];
            ArchbaseDataLogger.addAdditionalExtend(extendTemporal);
            if (extendTemporal.contains(".")) {
                additionalExtendsList.add(extendTemporal);
            } else {
                errorValidate = Boolean.TRUE;
                ArchbaseDataLogger.addError(String.format("'%s' não é um objeto válido!", extendTemporal));
            }
        }

        if (errorValidate) {
            throw new ArchbaseDataMojoException();
        }
    }

}
