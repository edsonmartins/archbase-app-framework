package br.com.archbase.validation.ddd.model;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.thirdparty.com.google.common.base.Supplier;
import com.tngtech.archunit.thirdparty.com.google.common.base.Suppliers;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper em torno de {@link JavaClass} que permite a criação de nomes formatados adicionais.
 */
class FormatableJavaClass {

    private static final Map<JavaClass, FormatableJavaClass> CACHE = new ConcurrentHashMap<>();

    private final JavaClass type;
    private final Supplier<String> abbreviatedName;

    private FormatableJavaClass(JavaClass type) {

        Assert.notNull(type, "JavaClass não deve ser nulo!");

        this.type = type;
        this.abbreviatedName = Suppliers.memoize(() -> {

            String abbreviatedPackage = Stream //
                    .of(type.getPackageName().split("\\.")) //
                    .map(it -> it.substring(0, 1)) //
                    .collect(Collectors.joining("."));

            return abbreviatedPackage.concat(".") //
                    .concat(ClassUtils.getShortName(type.getName()));
        });
    }

    /**
     * Cria um novo {@link FormatableJavaClass} para o {@link JavaClass} fornecido.
     *
     * @param type não deve ser {@literal null}.
     * @return
     */
    public static FormatableJavaClass of(JavaClass type) {

        Assert.notNull(type, "JavaClass não deve ser nulo!");

        return CACHE.computeIfAbsent(type, FormatableJavaClass::new);
    }

    /**
     * Retorna o nome completo abreviado (ou seja, cada fragmento de pacote reduzido ao seu primeiro caractere), por exemplo,
     * {@code com.archbase.MyType} vai resultar em {@code c.a.MyType}.
     *
     * @return nunca será {@literal null}.
     */
    public String getAbbreviatedFullName() {
        return abbreviatedName.get();
    }

    /**
     * Retorna o nome completo do tipo.
     *
     * @return nunca será {@literal null}.
     */
    public String getFullName() {
        return type.getFullName();
    }
}