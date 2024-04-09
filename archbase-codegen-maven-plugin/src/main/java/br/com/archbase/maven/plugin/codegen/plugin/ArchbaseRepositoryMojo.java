package br.com.archbase.maven.plugin.codegen.plugin;

import br.com.archbase.maven.plugin.codegen.support.RepositoryTemplateSupport;
import br.com.archbase.maven.plugin.codegen.support.ScanningConfigurationSupport;
import br.com.archbase.maven.plugin.codegen.util.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;

import java.util.List;


@Mojo(name = "repositories", requiresDependencyResolution = ResolutionScope.COMPILE, requiresDependencyCollection = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.COMPILE)
@SuppressWarnings("unused")
public class ArchbaseRepositoryMojo extends CommonsMojo {

    @Parameter(defaultValue = "${project.compileClasspathElements}")
    private List<String> classpathElements;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        ArchbaseDataLogger.configure(getLog());

        this.validateField(Constants.ENTITY_PACKAGE);
        this.validateField(Constants.REPOSITORY_PACKAGE);
        this.validateField(Constants.EXTENDS);

        try {

            CustomResourceLoader resourceLoader = new CustomResourceLoader(project);
            resourceLoader.setPostfix(repositoryPostfix);
            resourceLoader.setOverwrite(overwrite);

            String absolutePath = GeneratorUtils.getAbsolutePath(repositoryPackage);
            if (absolutePath == null) {
                ArchbaseDataLogger.addError("Não foi possível definir o caminho absoluto dos repositórios");
                throw new ArchbaseDataMojoException();
            }

            ScanningConfigurationSupport scanningConfigurationSupport = new ScanningConfigurationSupport(entityPackage, onlyAnnotations);

            RepositoryTemplateSupport repositoryTemplateSupport = new RepositoryTemplateSupport(resourceLoader, additionalExtendsList, apiVersion, revisionNumberClass);
            repositoryTemplateSupport.initializeCreation(absolutePath, repositoryPackage, scanningConfigurationSupport.getCandidates(resourceLoader), entityPackage);

            ArchbaseDataLogger.printGeneratedTables(true);

        } catch (Exception e) {
            ArchbaseDataLogger.addError(e.getMessage());
            throw new ArchbaseDataMojoException(e.getMessage(), e);
        }
    }

}
