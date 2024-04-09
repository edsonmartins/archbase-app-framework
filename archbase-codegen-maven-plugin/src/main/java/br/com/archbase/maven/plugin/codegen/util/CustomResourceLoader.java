package br.com.archbase.maven.plugin.codegen.util;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class CustomResourceLoader implements ResourceLoader {

    private URLClassLoader urlClassLoader;
    private String postfix;
    private boolean overwrite;
    private String repositoryPackage;
    private String repositoryPostfix;
    private String servicePackage;
    private String servicePostfix;

    public CustomResourceLoader(MavenProject project) {
        try {
            List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
            List<URL> runtimeUrls = new ArrayList<>();

            for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                String element = runtimeClasspathElements.get(i);
                runtimeUrls.add(new File(element).toURI().toURL());
            }

            urlClassLoader = new URLClassLoader(runtimeUrls.toArray(new URL[]{}),
                    Thread.currentThread().getContextClassLoader());
        } catch (DependencyResolutionRequiredException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Resource getResource(String s) {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.urlClassLoader;
    }

    public URLClassLoader getUrlClassLoader() {
        return this.urlClassLoader;
    }

    public String getPostfix() {
        return postfix;
    }

    public CustomResourceLoader setPostfix(String postfix) {
        this.postfix = postfix;
        return this;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public CustomResourceLoader setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    public String getRepositoryPackage() {
        return repositoryPackage;
    }

    public CustomResourceLoader setRepositoryPackage(String repositoryPackage) {
        this.repositoryPackage = repositoryPackage;
        return this;
    }

    public String getRepositoryPostfix() {
        return repositoryPostfix;
    }

    public CustomResourceLoader setRepositoryPostfix(String repositoryPostfix) {
        this.repositoryPostfix = repositoryPostfix;
        return this;
    }

    public String getServicePackage() {
        return servicePackage;
    }

    public void setServicePackage(String servicePackage) {
        this.servicePackage = servicePackage;
    }

    public String getServicePostfix() {
        return servicePostfix;
    }

    public void setServicePostfix(String servicePostfix) {
        this.servicePostfix = servicePostfix;
    }
}
