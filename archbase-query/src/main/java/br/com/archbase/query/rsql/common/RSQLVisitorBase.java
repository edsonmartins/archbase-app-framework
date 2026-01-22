package br.com.archbase.query.rsql.common;

import br.com.archbase.query.rsql.parser.ast.RSQLVisitor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Map.Entry;

@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class RSQLVisitorBase<R, A> implements RSQLVisitor<R, A> {


    @Setter
    protected static final Map<Class, Class> primitiveToWrapper;
    @Setter
    protected static Map<Class, ManagedType> managedTypeMap;
    @Setter
    protected static Map<String, EntityManager> entityManagerMap;
    @Setter
    protected static Map<Class<?>, Map<String, String>> propertyRemapping;
    @Setter
    protected static Map<Class<?>, List<String>> propertyWhitelist;
    @Setter
    protected static Map<Class<?>, List<String>> propertyBlacklist;
    @Setter
    protected static ConfigurableConversionService defaultConversionService;

    static {
        Map<Class, Class> map = new HashMap<>();
        map.put(boolean.class, Boolean.class);
        map.put(byte.class, Byte.class);
        map.put(char.class, Character.class);
        map.put(double.class, Double.class);
        map.put(float.class, Float.class);
        map.put(int.class, Integer.class);
        map.put(long.class, Long.class);
        map.put(short.class, Short.class);
        map.put(void.class, Void.class);
        primitiveToWrapper = Collections.unmodifiableMap(map);
    }

    protected Map<Class, ManagedType> getManagedTypeMap() {
        return managedTypeMap != null ? managedTypeMap : Collections.emptyMap();
    }

    protected Map<String, EntityManager> getEntityManagerMap() {
        return entityManagerMap != null ? entityManagerMap : Collections.emptyMap();
    }

    public abstract Map<String, String> getPropertyPathMapper();

    public Map<Class<?>, Map<String, String>> getPropertyRemapping() {
        return propertyRemapping != null ? propertyRemapping : Collections.emptyMap();
    }

    @SuppressWarnings("all")
    protected Object convert(String source, Class targetType) {
        log.debug("convert(source:{},targetType:{})", source, targetType);

        Object object = null;
        try {
            if (defaultConversionService.canConvert(String.class, targetType)) {
                object = defaultConversionService.convert(source, targetType);
            } else if (targetType.equals(String.class)) {
                object = source;
            } else if (targetType.equals(UUID.class)) {
                object = UUID.fromString(source);
            } else if (targetType.equals(Date.class) || targetType.equals(java.sql.Date.class)) {
                object = java.sql.Date.valueOf(LocalDate.parse(source));
            } else if (targetType.equals(LocalDate.class)) {
                object = LocalDate.parse(source);
            } else if (targetType.equals(LocalDateTime.class)) {
                object = LocalDateTime.parse(source);
            } else if (targetType.equals(OffsetDateTime.class)) {
                object = OffsetDateTime.parse(source);
            } else if (targetType.equals(ZonedDateTime.class)) {
                object = ZonedDateTime.parse(source);
            } else if (targetType.equals(Character.class)) {
                object = (!StringUtils.isEmpty(source) ? source.charAt(0) : null);
            } else if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
                String normalized = source.trim().toLowerCase();
                if (normalized.equals("true") || normalized.equals("false")
                    || normalized.equals("1") || normalized.equals("0")) {
                    object = Boolean.valueOf(normalized.equals("true") || normalized.equals("1"));
                } else {
                    throw new IllegalArgumentException("Invalid boolean value: " + source);
                }
            } else if (targetType.isEnum()) {
                object = Enum.valueOf(targetType, source);
            } else {
                Constructor<?> cons = (Constructor<?>) targetType.getConstructor(new Class<?>[]{String.class});
                object = cons.newInstance(new Object[]{source});
            }

            return object;
        } catch (DateTimeParseException | IllegalArgumentException e) {
            log.debug("Analisando [{}] com [{}] causando [{}], pule", source, targetType.getName(), e.getMessage());
        } catch (Exception e) {
            log.error("Analisando [{}] com [{}] causando [{}], adicione seu analisador via RSQLSupport.addConverter(Type.class, Type::valueOf)", source, targetType.getName(), e.getMessage(), e);
        }
        return null;
    }

    protected void accessControl(Class type, String name) {
        log.info("accessControl(type:{},name:{})", type, name);

        if (propertyWhitelist != null && propertyWhitelist.containsKey(type) && !propertyWhitelist.get(type).contains(name)) {
            throw new IllegalArgumentException("Propriedade " + type.getName() + "." + name + " não está na lista de permissões");
        }

        if (propertyBlacklist != null && propertyBlacklist.containsKey(type) && propertyBlacklist.get(type).contains(name)) {
            throw new IllegalArgumentException("Propriedade " + type.getName() + "." + name + " está na lista negra");
        }
    }

    protected String mapPropertyPath(String propertyPath) {
        if (!getPropertyPathMapper().isEmpty()) {
            String property = getPropertyPathMapper().get(propertyPath);
            if (StringUtils.hasText(property)) {
                log.debug("Map propertyPath [{}] para [{}]", propertyPath, property);
                return property;
            }
        }
        return propertyPath;
    }

    protected String mapProperty(String selector, Class<?> entityClass) {
        if (!getPropertyRemapping().isEmpty()) {
            Map<String, String> map = getPropertyRemapping().get(entityClass);
            String property = (map != null) ? map.get(selector) : null;
            if (StringUtils.hasText(property)) {
                log.debug("Map property [{}] de [{}] para [{}]", selector, property, entityClass);
                return property;
            }
        }
        return selector;
    }

    protected <T> Class<?> findPropertyType(String property, ManagedType<T> classMetadata) {
        Class<?> propertyType = null;
        if (classMetadata.getAttribute(property).isCollection()) {
            propertyType = ((PluralAttribute) classMetadata.getAttribute(property)).getBindableJavaType();
        } else {
            propertyType = classMetadata.getAttribute(property).getJavaType();
        }
        return propertyType;
    }

    @SneakyThrows(Exception.class)
    @SuppressWarnings("all")
    protected <T> ManagedType<T> getManagedType(Class<T> cls) {
        Exception ex = null;
        if (getEntityManagerMap().size() > 0) {
            ManagedType<T> managedType = getManagedTypeMap().get(cls);
            if (managedType != null) {
                log.debug("Tipo gerenciado encontrado [{}] no cache", cls);
                return managedType;
            }
            for (Entry<String, EntityManager> entityManagerEntry : getEntityManagerMap().entrySet()) {
                try {
                    managedType = entityManagerEntry.getValue().getMetamodel().managedType(cls);
                    getManagedTypeMap().put(cls, managedType);
                    log.info("Tipo gerenciado encontrado [{}] em EntityManager [{}]", cls, entityManagerEntry.getKey());
                    return managedType;
                } catch (Exception e) {
                    if (e != null) {
                        ex = e;
                    }
                    log.debug("[{}] não encontrado em EntityManager [{}] devido a [{}]", cls, entityManagerEntry.getKey(), e == null ? "-" : e.getMessage());
                }
            }
        }
        log.error("[{}] não encontrado em EntityManager{}: [{}]", cls, getEntityManagerMap().size() > 1 ? "s" : "", StringUtils.collectionToCommaDelimitedString(getEntityManagerMap().keySet()));
        throw ex != null ? ex : new IllegalStateException("Nenhum bean de gerenciador de entidade encontrado no contexto do aplicativo");
    }

    protected <T> ManagedType<T> getManagedElementCollectionType(String mappedProperty, ManagedType<T> classMetadata) {
        try {
            Class<?> cls = findPropertyType(mappedProperty, classMetadata);
            if (!cls.isPrimitive() && !primitiveToWrapper.containsValue(cls) && !cls.equals(String.class) && getEntityManagerMap().size() > 0) {
                ManagedType<T> managedType = getManagedTypeMap().get(cls);
                if (managedType != null) {
                    log.debug("Tipo gerenciado encontrado [{}] no cache", cls);
                    return managedType;
                }
                Iterator<Entry<String, EntityManager>> iterator = getEntityManagerMap().entrySet().iterator();
                if (iterator.hasNext()) {
                    Entry<String, EntityManager> entityManagerEntry = iterator.next();
                    managedType = (ManagedType<T>) entityManagerEntry.getValue().getMetamodel().managedType(cls);
                    getManagedTypeMap().put(cls, managedType);
                    log.info("Tipo gerenciado encontrado [{}] em EntityManager [{}]", cls, entityManagerEntry.getKey());
                    return managedType;
                }

            }
        } catch (Exception e) {
            log.warn("Não é possível obter o tipo gerenciado de [{}]", mappedProperty, e);
        }
        return classMetadata;
    }

    protected <T> boolean hasPropertyName(String property, ManagedType<T> classMetadata) {
        Set<Attribute<? super T, ?>> names = classMetadata.getAttributes();
        for (Attribute<? super T, ?> name : names) {
            if (name.getName().equals(property))
                return true;
        }
        return false;
    }

    @SneakyThrows
    protected Class getElementCollectionGenericType(Class type, Attribute attribute) {
        Member member = attribute.getJavaMember();
        if (member instanceof Field) {
            Field field = (Field) member;
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType rawType = (ParameterizedType) genericType;
                Class elementCollectionClass = Class.forName(rawType.getActualTypeArguments()[0].getTypeName());
                log.info("Mapear o tipo genérico de coleção de elementos [{}] para [{}]", attribute.getName(), elementCollectionClass);
                return elementCollectionClass;
            }
        }
        return type;
    }

    protected <T> boolean isEmbeddedType(String property, ManagedType<T> classMetadata) {
        return classMetadata.getAttribute(property).getPersistentAttributeType() == PersistentAttributeType.EMBEDDED;
    }

    protected <T> boolean isElementCollectionType(String property, ManagedType<T> classMetadata) {
        return classMetadata.getAttribute(property).getPersistentAttributeType() == PersistentAttributeType.ELEMENT_COLLECTION;
    }

    protected <T> boolean isAssociationType(String property, ManagedType<T> classMetadata) {
        return classMetadata.getAttribute(property).isAssociation();
    }

    protected <T> boolean isOneToOneAssociationType(String property, ManagedType<T> classMetadata) {
        return classMetadata.getAttribute(property).isAssociation()
                && PersistentAttributeType.ONE_TO_ONE == classMetadata.getAttribute(property).getPersistentAttributeType();
    }

    protected <T> boolean isOneToManyAssociationType(String property, ManagedType<T> classMetadata) {
        return classMetadata.getAttribute(property).isAssociation()
                && PersistentAttributeType.ONE_TO_MANY == classMetadata.getAttribute(property).getPersistentAttributeType();
    }

}
