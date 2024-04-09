package br.com.archbase.maven.plugin.codegen.support.maker;

import br.com.archbase.maven.plugin.codegen.support.maker.builder.ObjectBuilder;
import br.com.archbase.maven.plugin.codegen.support.maker.builder.ObjectStructure;
import br.com.archbase.maven.plugin.codegen.support.maker.builder.ObjectStructure.ObjectFunction;
import br.com.archbase.maven.plugin.codegen.support.maker.values.ExpressionValues;
import br.com.archbase.maven.plugin.codegen.support.maker.values.ObjectTypeValues;
import br.com.archbase.maven.plugin.codegen.support.maker.values.ObjectValues;
import br.com.archbase.maven.plugin.codegen.support.maker.values.ScopeValues;
import br.com.archbase.maven.plugin.codegen.util.*;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("all")
public class ResourceStructure {

    public static final String OVERRIDE = "Override";
    private final static Map<Class<?>, Class<?>> mapConvert = new HashMap<>();
    private ObjectBuilder objectBuilder;
    private CustomResourceLoader loader;
    private Integer error = 0;

    public ResourceStructure(String resourcePackage, String entityName, String entityClass, String postfix,
                             String servicePackage, String servicePostfix, CustomResourceLoader loader, String additionalPackage, String apiVersion, String revisionNumberClass) {

        this.loader = loader;
        String resourceName = entityName + postfix;
        String serviceName = entityName + servicePostfix;
        String serviceNameAttribute = GeneratorUtils.decapitalize(serviceName);
        Tuple<String, Boolean> entityId = getEntityId(entityClass);
        if (entityId != null) {
            ObjectFunction getServiceTemp = new ObjectFunction(ScopeValues.PUBLIC, "getService", "CommonArchbaseService<" + entityName + "," + GeneratorUtils.getSimpleClassName(entityId.left()) + "," + GeneratorUtils.getSimpleClassName(revisionNumberClass) + ">");
            getServiceTemp.addAnnotation(OVERRIDE);
            getServiceTemp.setFunctionReturn(serviceNameAttribute);

            ObjectFunction getRepositoryTemp = new ObjectFunction(ScopeValues.PUBLIC, "getRepository", "Repository<" + entityName + "," + GeneratorUtils.getSimpleClassName(entityId.left()) + "," + GeneratorUtils.getSimpleClassName(revisionNumberClass) + ">");
            getRepositoryTemp.addAnnotation(OVERRIDE);
            getRepositoryTemp.setFunctionReturn(serviceNameAttribute + ".getRepository()");

            ObjectFunction createConcretePageTemp = new ObjectFunction(ScopeValues.PROTECTED, "createConcretePage", "Page<" + entityName + ">");
            createConcretePageTemp.addAnnotation(OVERRIDE);
            createConcretePageTemp.addArgument("List<" + entityName + ">", "content");
            createConcretePageTemp.addArgument("Pageable", "pageRequest");
            createConcretePageTemp.addArgument("long", "totalElements");
            createConcretePageTemp.setFunctionReturn("new  Page" + entityName + "(content, pageRequest, totalElements)");

            ObjectFunction createConcreteListTemp = new ObjectFunction(ScopeValues.PROTECTED, "createConcreteList", "List<" + entityName + ">");
            createConcreteListTemp.addArgument("List<" + entityName + ">", "result");
            createConcreteListTemp.addAnnotation(OVERRIDE);
            createConcreteListTemp.setFunctionReturn("new List" + entityName + "(result)");

            String rawBody = "" +
                    "	class Page" + entityName + " extends PageImpl<" + entityName + "> {" +
                    "		public Page" + entityName + "(List<" + entityName + "> content) {" +
                    "			super(content);" +
                    "		}" +
                    "" +
                    "		public Page" + entityName + "(List<" + entityName + "> content, Pageable pageable, long total) {" +
                    "			super(content, pageable, total);" +
                    "		}" +
                    "	}"
                    + "    class List" + entityName + " extends ArrayList<" + entityName + ">{" +
                    "		public List" + entityName + "(Collection<? extends " + entityName + "> c) {" +
                    "			super(c);" +
                    "		}" +
                    "	}";


            this.objectBuilder = new ObjectBuilder(
                    new ObjectStructure(resourcePackage, ScopeValues.PUBLIC, ObjectTypeValues.CLASS, resourceName)
                            .addExtend("CommonArchbaseRestController", entityName,
                                    GeneratorUtils.getSimpleClassName(entityId.left()), GeneratorUtils.getSimpleClassName(revisionNumberClass))
                            .addImport(servicePackage + "."
                                    + (additionalPackage.isEmpty() ? "" : (additionalPackage + ".")) + serviceName)
                            .addImport(entityClass)
                            .addImport(entityId.left())
                            .addImport("br.com.archbase.ddd.infraestructure.resource.CommonArchbaseRestController")
                            .addImport("br.com.archbase.ddd.infraestructure.service.CommonArchbaseService")
                            .addImport("br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository")
                            .addImport("br.com.archbase.ddd.domain.contracts.Repository")
                            .addImport("org.springframework.web.bind.annotation.RequestMapping")
                            .addImport("org.springframework.web.bind.annotation.CrossOrigin")
                            .addImport("br.com.archbase.resource.logger.annotation.Logging")
                            .addImport("io.swagger.annotations.Api")
                            .addImport("org.springframework.data.domain.Page")
                            .addImport("org.springframework.data.domain.Pageable")
                            .addImport("org.springframework.data.domain.PageImpl")
                            .addImport("java.util.List")
                            .addImport(revisionNumberClass)
                            .addImport("java.util.Collection")
                            .addImport("java.util.ArrayList")
                            .addImport(Autowired.class)
                            .addImport("org.springframework.web.bind.annotation.RestController")
                            .addAnnotation("@SuppressWarnings(\"serial\")")
                            .addAnnotation("CrossOrigin(origins = \"*\")")
                            .addAnnotation("Api(value=" + "\"" + entityName + "\")")
                            .addAnnotation("Logging")
                            .addAnnotation("RestController")
                            .addAnnotation("RequestMapping(value = \"/api.v" + apiVersion + "/" + firstCharToLowerCase(entityName) + "\")")
                            .addAttribute(serviceName, serviceNameAttribute)
                            .addConstructor(new ObjectStructure.ObjectConstructor(ScopeValues.PUBLIC, resourceName)
                                    .addAnnotation(Autowired.class).addArgument(serviceName, serviceNameAttribute)
                                    .addBodyLine(ObjectValues.THIS.getValue() + serviceNameAttribute
                                            + ExpressionValues.EQUAL.getValue() + serviceNameAttribute))
                            .addFunction(getServiceTemp)
                            .addFunction(getRepositoryTemp)
                            .addFunction(createConcretePageTemp)
                            .addFunction(createConcreteListTemp)
                            .setObjectRawBody(rawBody)


            )
                    .setAttributeBottom(false);
        }

    }

    private String firstCharToLowerCase(String str) {

        if (str == null || str.length() == 0)
            return "";

        if (str.length() == 1)
            return str.toLowerCase();

        char[] chArr = str.toCharArray();
        chArr[0] = Character.toLowerCase(chArr[0]);

        return new String(chArr);
    }

    @SuppressWarnings("unchecked")
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

            error = ArchbaseDataLogger.addError("Resource Error: Primary key not found in "
                    + GeneratorUtils.getSimpleClassName(entityClass) + ".java");
            return null;
        } catch (GeneratorException ex) {
            error = ArchbaseDataLogger.addError(ex.getMessage());
            return null;
        } catch (Exception e) {
            error = ArchbaseDataLogger.addError("Resource Error: Failed to access entity "
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
