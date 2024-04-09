package br.com.archbase.maven.plugin.codegen.plugin;

import br.com.archbase.maven.plugin.codegen.support.ScanningConfigurationSupport;
import br.com.archbase.maven.plugin.codegen.support.ServiceTemplateSupport;
import br.com.archbase.maven.plugin.codegen.util.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


@Mojo(name = "services", requiresDependencyResolution = ResolutionScope.COMPILE, requiresDependencyCollection = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.COMPILE)
@SuppressWarnings("unused")
public class ArchbaseDataServiceMojo extends CommonsMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        ArchbaseDataLogger.configure(getLog());

        this.validateField(Constants.ENTITY_PACKAGE);
        this.validateField(Constants.SERVICE_PACKAGE);
        this.validateField(Constants.REPOSITORY_PACKAGE);

        try {
            CustomResourceLoader resourceLoader = new CustomResourceLoader(project);
            resourceLoader.setPostfix(servicePostfix);
            resourceLoader.setRepositoryPackage(repositoryPackage);
            resourceLoader.setRepositoryPostfix(repositoryPostfix);
            resourceLoader.setOverwrite(overwrite);

            String absolutePath = GeneratorUtils.getAbsolutePath(servicePackage);
            if (absolutePath == null) {
                ArchbaseDataLogger.addError("Não foi possível definir o caminho absoluto dos serviços");
                throw new ArchbaseDataMojoException();
            }

            ScanningConfigurationSupport scanningConfigurationSupport = new ScanningConfigurationSupport(entityPackage, onlyAnnotations);

            ServiceTemplateSupport serviceTemplateSupport = new ServiceTemplateSupport(resourceLoader, apiVersion, revisionNumberClass);
            serviceTemplateSupport.initializeCreation(absolutePath, servicePackage, scanningConfigurationSupport.getCandidates(resourceLoader), entityPackage);

            ArchbaseDataLogger.printGeneratedTables(true);

        } catch (Exception e) {
            ArchbaseDataLogger.addError(e.getMessage());
            throw new ArchbaseDataMojoException(e.getMessage(), e);
        }
    }

}
