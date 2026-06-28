package br.com.archbase.query.rsql.common;

import br.com.archbase.query.rsql.parser.RSQLParser;
import br.com.archbase.query.rsql.parser.ast.ComparisonOperator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.ManagedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@SuppressWarnings({"rawtypes"})
@Getter
public class RSQLCommonSupport {

    private static final Map<String, EntityManager> entityManagerMap = new ConcurrentHashMap<>();
    private static final Map<Class, ManagedType> managedTypeMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, String>> propertyRemapping = new ConcurrentHashMap<>();
    private static final Map<Class, Class> valueTypeMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<String>> propertyWhitelist = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<String>> propertyBlacklist = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Set<ComparisonOperator>>> operatorWhitelist = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Set<ComparisonOperator>>> operatorBlacklist = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Set<String>> protectedSelectors = new ConcurrentHashMap<>();
    private static volatile int maxPageSize = 1000;
    private static final ConfigurableConversionService conversionService = new DefaultConversionService();

    public RSQLCommonSupport() {
        init();
    }

    public RSQLCommonSupport(Map<String, EntityManager> entityManagerMap) {
        if (entityManagerMap != null) {
            RSQLCommonSupport.entityManagerMap.putAll(entityManagerMap);
            log.info("{} EntityManager bean{} encontrado: {}", entityManagerMap.size(), entityManagerMap.size() > 1 ? "s são" : " é", entityManagerMap);
        } else {
            log.warn("Nenhum bean EntityManager foi encontrado");
        }
        init();
    }

    public static Map<String, EntityManager> getEntityManagerMap() {
        return entityManagerMap;
    }

    public static Map<Class, ManagedType> getManagedTypeMap() {
        return managedTypeMap;
    }

    public static Map<Class<?>, Map<String, String>> getPropertyRemapping() {
        return propertyRemapping;
    }

    public static Map<Class, Class> getValueTypeMap() {
        return valueTypeMap;
    }

    public static Map<Class<?>, List<String>> getPropertyWhitelist() {
        return propertyWhitelist;
    }

    public static Map<Class<?>, List<String>> getPropertyBlacklist() {
        return propertyBlacklist;
    }

    public static Map<Class<?>, Map<String, Set<ComparisonOperator>>> getOperatorWhitelist() {
        return operatorWhitelist;
    }

    public static Map<Class<?>, Map<String, Set<ComparisonOperator>>> getOperatorBlacklist() {
        return operatorBlacklist;
    }

    public static Map<Class<?>, Set<String>> getProtectedSelectors() {
        return protectedSelectors;
    }

    public static int getMaxPageSize() {
        return maxPageSize;
    }

    /**
     * Define o tamanho máximo de página aceito pelos endpoints de consulta. Valores menores ou
     * iguais a zero desativam o limite. Padrão: {@code 1000}.
     */
    public static void setMaxPageSize(int value) {
        log.info("Definindo tamanho máximo de página para {}", value);
        maxPageSize = value;
    }

    public static ConfigurableConversionService getConversionService() {
        return conversionService;
    }

    public static void clear() {
        entityManagerMap.clear();
        managedTypeMap.clear();
        propertyRemapping.clear();
        valueTypeMap.clear();
        propertyWhitelist.clear();
        propertyBlacklist.clear();
        operatorWhitelist.clear();
        operatorBlacklist.clear();
        protectedSelectors.clear();
    }

    public static void addConverter(Converter<?, ?> converter) {
        conversionService.addConverter(converter);
    }

    public static <T> void addConverter(Class<T> targetType, Converter<String, ? extends T> converter) {
        log.info("Adicionando conversor de entidade para {}", targetType);
        conversionService.addConverter(String.class, targetType, converter);
    }

    public static <T> void removeConverter(Class<T> targetType) {
        log.info("Removendo conversor de entidade para {}", targetType);
        conversionService.removeConvertible(String.class, targetType);
    }

    public static void addPropertyWhitelist(Class<?> entityClass, List<String> propertyList) {
        propertyWhitelist.computeIfAbsent(entityClass, entityClazz -> new ArrayList<>()).addAll(propertyList);
    }

    public static void addPropertyWhitelist(Class<?> entityClass, String property) {
        propertyWhitelist.computeIfAbsent(entityClass, entityClazz -> new ArrayList<>()).add(property);
    }

    public static void addPropertyBlacklist(Class<?> entityClass, List<String> propertyList) {
        propertyBlacklist.computeIfAbsent(entityClass, entityClazz -> new ArrayList<>()).addAll(propertyList);
    }

    public static void addPropertyBlacklist(Class<?> entityClass, String property) {
        propertyBlacklist.computeIfAbsent(entityClass, entityClazz -> new ArrayList<>()).add(property);
    }

    /**
     * Declara a lista de operadores permitidos para uma propriedade. Quando definida, qualquer
     * operador fora desta lista é rejeitado para a propriedade. Útil, por exemplo, para liberar
     * apenas {@code ==} em uma coluna e bloquear {@code =like=}.
     */
    public static void addOperatorWhitelist(Class<?> entityClass, String property, ComparisonOperator... operators) {
        operatorWhitelist
                .computeIfAbsent(entityClass, c -> new ConcurrentHashMap<>())
                .computeIfAbsent(property, p -> new LinkedHashSet<>())
                .addAll(Arrays.asList(operators));
    }

    /**
     * Declara operadores proibidos para uma propriedade (ex.: bloquear {@code =like=}/{@code =ilike=}
     * em uma coluna sem índice). Operadores não listados continuam permitidos.
     */
    public static void addOperatorBlacklist(Class<?> entityClass, String property, ComparisonOperator... operators) {
        operatorBlacklist
                .computeIfAbsent(entityClass, c -> new ConcurrentHashMap<>())
                .computeIfAbsent(property, p -> new LinkedHashSet<>())
                .addAll(Arrays.asList(operators));
    }

    /**
     * Declara seletores "protegidos" (ex.: {@code tenantId}, campos de autorização) que não podem
     * ser anulados por uma expressão {@code OR} no filtro RSQL. Enquanto nenhum seletor for
     * registrado para a entidade, a análise de bypass é um no-op (sem mudança de comportamento).
     *
     * @see RSQLOrBypassAnalyzer
     */
    public static void addProtectedSelector(Class<?> entityClass, String... selectors) {
        protectedSelectors
                .computeIfAbsent(entityClass, c -> ConcurrentHashMap.newKeySet())
                .addAll(Arrays.asList(selectors));
    }

    public static MultiValueMap<String, String> toMultiValueMap(final String rsqlQuery) {
        log.debug("toMultiValueMap(rsqlQuery:{})", rsqlQuery);
        MultiValueMap<String, String> map = CollectionUtils.toMultiValueMap(new HashMap<>());
        if (StringUtils.hasText(rsqlQuery)) {
            new RSQLParser(RSQLOperators.supportedOperators()).parse(rsqlQuery).accept(new RSQLSimpleConverter(), map);
        }
        return map;
    }

    public static Map<String, MultiValueMap<String, String>> toComplexMultiValueMap(final String rsqlQuery) {
        log.debug("toComplexMultiValueMap(rsqlQuery:{})", rsqlQuery);
        Map<String, MultiValueMap<String, String>> map = new HashMap<>();
        if (StringUtils.hasText(rsqlQuery)) {
            new RSQLParser(RSQLOperators.supportedOperators()).parse(rsqlQuery).accept(new RSQLComplexConverter(), map);
        }
        return map;
    }

    public static void addMapping(Class<?> entityClass, Map<String, String> mapping) {
        log.info("Adicionando mapeamento de classe de entidade para {}", entityClass);
        propertyRemapping.put(entityClass, mapping);
    }

    public static void addMapping(Class<?> entityClass, String selector, String property) {
        log.info("Adicionando mapeamento de classe de entidade para {}, seletor {} e propriedade {}", entityClass, selector, property);
        propertyRemapping.computeIfAbsent(entityClass, entityClazz -> new ConcurrentHashMap<>()).put(selector, property);
    }

    public static <T> void addEntityAttributeParser(Class<T> valueClass, Function<String, ? extends T> function) {
        log.info("Adicionando analisador de atributo de entidade para {}", valueClass);
        if (valueClass != null && function != null) {
            addConverter(valueClass, function::apply);
        }
    }

    public static void addEntityAttributeTypeMap(Class valueClass, Class mappedClass) {
        log.info("Adicionando mapa de tipo de atributo de entidade para {} -> {}", valueClass, mappedClass);
        if (valueClass != null && mappedClass != null) {
            valueTypeMap.put(valueClass, mappedClass);
        }
    }

    protected void init() {
        conversionService.removeConvertible(Object.class, Object.class);
        RSQLVisitorBase.setEntityManagerMap(getEntityManagerMap());
        RSQLVisitorBase.setManagedTypeMap(getManagedTypeMap());
        RSQLVisitorBase.setPropertyRemapping(getPropertyRemapping());
        RSQLVisitorBase.setPropertyWhitelist(getPropertyWhitelist());
        RSQLVisitorBase.setPropertyBlacklist(getPropertyBlacklist());
        RSQLVisitorBase.setOperatorWhitelist(getOperatorWhitelist());
        RSQLVisitorBase.setOperatorBlacklist(getOperatorBlacklist());
        RSQLVisitorBase.setDefaultConversionService(getConversionService());
        log.info("RSQLCommonSupport {} foi inicializado");
    }


}
