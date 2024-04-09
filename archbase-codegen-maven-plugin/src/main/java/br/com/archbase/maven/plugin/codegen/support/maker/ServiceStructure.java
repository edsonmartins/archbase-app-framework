package br.com.archbase.maven.plugin.codegen.support.maker;

import br.com.archbase.maven.plugin.codegen.support.maker.builder.ObjectBuilder;
import br.com.archbase.maven.plugin.codegen.support.maker.builder.ObjectStructure;
import br.com.archbase.maven.plugin.codegen.support.maker.values.ObjectTypeValues;
import br.com.archbase.maven.plugin.codegen.support.maker.values.ObjectValues;
import br.com.archbase.maven.plugin.codegen.support.maker.values.ScopeValues;
import br.com.archbase.maven.plugin.codegen.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("all")
public class ServiceStructure {

    private final static Map<Class<?>, Class<?>> mapConvert = new HashMap<>();
    private ObjectBuilder objectBuilder;
    private CustomResourceLoader loader;
    private int error;

    public ServiceStructure(String managerPackage, String entityName, String entityClass, String postfix,
                            String repositoryPackage, String repositoryPostfix, CustomResourceLoader loader, String additionalPackage, String apiVersion, String revisionNumberClass) {

        this.loader = loader;
        String serviceName = entityName + postfix;
        String repositoryName = entityName + repositoryPostfix;
        String repositoryNameAttribute = GeneratorUtils.decapitalize(repositoryName);

        Tuple<String, Boolean> entityId = getEntityId(entityClass);
        if (entityId != null) {
            this.objectBuilder = new ObjectBuilder(
                    new ObjectStructure(managerPackage, ScopeValues.PUBLIC, ObjectTypeValues.CLASS, serviceName)
                            .addExtend("CommonArchbaseService", entityName,
                                    GeneratorUtils.getSimpleClassName(entityId.left()), GeneratorUtils.getSimpleClassName(revisionNumberClass))
                            .addImport(entityId.left())
                            .addImport(revisionNumberClass)
                            .addImport(repositoryPackage + "."
                                    + (additionalPackage.isEmpty() ? "" : (additionalPackage + ".")) + repositoryName)
                            .addImport(entityClass).addImport(Autowired.class).addImport(Service.class)
                            .addImport("br.com.archbase.ddd.infraestructure.service.CommonArchbaseService")
                            .addAnnotation(Service.class)
                            .addConstructor(new ObjectStructure.ObjectConstructor(ScopeValues.PUBLIC, serviceName)
                                    .addAnnotation(Autowired.class).addArgument(repositoryName, repositoryNameAttribute)
                                    .addBodyLine(ObjectValues.SUPER.getValue() + repositoryNameAttribute + ")")))
                    .setAttributeBottom(false);
        }

    }


    private Tuple<String, Boolean> getEntityId(String entityClass) {
        try {
            Class<?> entity;
            if (loader == null) {
                entity = Class.forName(entityClass);
            } else {
                entity = loader.getUrlClassLoader().loadClass(entityClass);
            }

            while (entity != null) {
                for (Field field : entity.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                        Class<?> dataType = field.getType();
                        if (field.getType().isPrimitive()) {
                            dataType = this.primitiveToObject(field.getType());
                        }
                        return new Tuple<>(dataType.getName(), this.isCustomType(dataType));
                    }
                }

                for (Method method : entity.getDeclaredMethods()) {
                    if (!method.getReturnType().equals(Void.TYPE)
                            && (method.isAnnotationPresent(Id.class) || method.isAnnotationPresent(EmbeddedId.class))) {
                        Class<?> dataType = method.getReturnType();
                        if (method.getReturnType().isPrimitive()) {
                            dataType = this.primitiveToObject(method.getReturnType());
                        }
                        return new Tuple<>(dataType.getName(), this.isCustomType(dataType));
                    }
                }
                entity = entity.getSuperclass();
            }

            error = ArchbaseDataLogger.addError("Repository Error: Primary key not found in "
                    + GeneratorUtils.getSimpleClassName(entityClass) + ".java");
            return null;
        } catch (GeneratorException ex) {
            error = ArchbaseDataLogger.addError(ex.getMessage());
            return null;
        } catch (Exception e) {
            error = ArchbaseDataLogger.addError("Repository Error: Failed to access entity "
                    + GeneratorUtils.getSimpleClassName(entityClass) + ".java");
            return null;
        }
    }

    public Tuple<String, Integer> build() {
        return new Tuple<>(objectBuilder == null ? null : objectBuilder.build(), 0);
    }

    private boolean isCustomType(Class<?> clazz) {
        return !clazz.isAssignableFrom(Boolean.class) && !clazz.isAssignableFrom(Byte.class)
                && !clazz.isAssignableFrom(String.class) && !clazz.isAssignableFrom(Integer.class)
                && !clazz.isAssignableFrom(Long.class) && !clazz.isAssignableFrom(Float.class)
                && !clazz.isAssignableFrom(Double.class);
    }

    private Class<?> primitiveToObject(Class<?> clazz) {
        Class<?> convertResult = mapConvert.get(clazz);
        if (convertResult == null) {
            throw new GeneratorException("Type parameter '" + clazz.getName() + "' is incorrect");
        }
        return convertResult;
    }
}
