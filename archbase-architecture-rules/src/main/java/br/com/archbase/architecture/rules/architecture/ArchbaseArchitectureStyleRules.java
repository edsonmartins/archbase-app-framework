package br.com.archbase.architecture.rules.architecture;

import br.com.archbase.hexagonal.Adapter;
import br.com.archbase.hexagonal.Application;
import br.com.archbase.hexagonal.Port;
import br.com.archbase.hexagonal.PrimaryAdapter;
import br.com.archbase.hexagonal.PrimaryPort;
import br.com.archbase.hexagonal.SecondaryAdapter;
import br.com.archbase.hexagonal.SecondaryPort;
import br.com.archbase.layered.ApplicationLayer;
import br.com.archbase.layered.DomainLayer;
import br.com.archbase.layered.InfrastructureLayer;
import br.com.archbase.layered.InterfaceLayer;
import br.com.archbase.onion.classical.ApplicationServiceRing;
import br.com.archbase.onion.classical.DomainModelRing;
import br.com.archbase.onion.classical.DomainServiceRing;
import br.com.archbase.onion.simplified.ApplicationRing;
import br.com.archbase.onion.simplified.DomainRing;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Regras ArchUnit que fazem valer as anotações de estilo arquitetural do módulo {@code archbase-architecture}
 * (camadas, onion clássica, onion simplificada e hexagonal).
 * <p>
 * Até aqui essas anotações eram só documentação: nenhuma regra as consultava. Inspirado nas regras equivalentes do
 * jMolecules ({@code JMoleculesArchitectureRules}).
 * <p>
 * <b>Como um tipo ganha uma camada/anel/papel:</b> pela anotação mais próxima — primeiro o próprio tipo, depois o
 * seu pacote ({@code package-info.java}) e então os pacotes pais, subindo até a raiz. Anotar o pacote raiz de cada
 * camada é o uso esperado:
 * <pre>{@code
 * // src/main/java/com/acme/app/domain/package-info.java
 * @DomainLayer
 * package com.acme.app.domain;
 * }</pre>
 * Tipos sem anotação em lugar nenhum da cadeia ficam de fora — a regra não acusa nada sobre eles. Por isso ligar
 * estas regras num projeto que não usa as anotações é inofensivo.
 * <p>
 * Nenhuma destas regras é ligada por padrão: use os métodos {@code with...ArchitectureRules()} de
 * {@link br.com.archbase.architecture.rules.core.ArchbaseArchitectureRules} ou registre a {@link ArchRule} direto.
 *
 * @since 3.4.6
 */
public final class ArchbaseArchitectureStyleRules {

    private static final List<Class<? extends Annotation>> LAYERS = List.of(
            InterfaceLayer.class, ApplicationLayer.class, DomainLayer.class, InfrastructureLayer.class);

    private static final List<Class<? extends Annotation>> ONION_CLASSICAL = List.of(
            br.com.archbase.onion.classical.InfrastructureRing.class, ApplicationServiceRing.class,
            DomainServiceRing.class, DomainModelRing.class);

    private static final List<Class<? extends Annotation>> ONION_SIMPLIFIED = List.of(
            br.com.archbase.onion.simplified.InfrastructureRing.class, ApplicationRing.class, DomainRing.class);

    /**
     * Do mais específico para o mais genérico: um tipo em pacote {@code @PrimaryAdapter} é primário mesmo que
     * {@code @PrimaryAdapter} seja meta-anotada com {@code @Adapter}.
     */
    private static final List<Class<? extends Annotation>> HEXAGONAL = List.of(
            PrimaryAdapter.class, SecondaryAdapter.class, Adapter.class,
            PrimaryPort.class, SecondaryPort.class, Port.class, Application.class);

    private ArchbaseArchitectureStyleRules() {
    }

    /**
     * Arquitetura em camadas (Evans), modo relaxado: cada camada só depende dela mesma ou de camadas abaixo, em
     * qualquer distância. Ordem de cima para baixo: {@link InterfaceLayer}, {@link ApplicationLayer},
     * {@link DomainLayer}, {@link InfrastructureLayer}.
     */
    public static ArchRule layered() {
        return orderedRule("Arquitetura em camadas", LAYERS, false,
                "camadas só podem depender de si mesmas ou de camadas inferiores");
    }

    /**
     * Arquitetura em camadas, modo estrito: cada camada só depende dela mesma ou da camada <em>imediatamente</em>
     * abaixo.
     */
    public static ArchRule layeredStrict() {
        return orderedRule("Arquitetura em camadas (estrita)", LAYERS, true,
                "camadas só podem depender de si mesmas ou da camada imediatamente inferior");
    }

    /**
     * Onion clássica (Palermo): anéis internos não dependem de anéis externos. De fora para dentro:
     * {@code InfrastructureRing}, {@link ApplicationServiceRing}, {@link DomainServiceRing}, {@link DomainModelRing}.
     */
    public static ArchRule onionClassical() {
        return orderedRule("Arquitetura onion clássica", ONION_CLASSICAL, false,
                "anéis internos não podem depender de anéis externos");
    }

    /**
     * Onion simplificada: de fora para dentro {@code InfrastructureRing}, {@link ApplicationRing},
     * {@link DomainRing}.
     */
    public static ArchRule onionSimplified() {
        return orderedRule("Arquitetura onion simplificada", ONION_SIMPLIFIED, false,
                "anéis internos não podem depender de anéis externos");
    }

    /**
     * Arquitetura hexagonal (ports and adapters):
     * <ul>
     * <li>a aplicação ({@link Application}) e as portas ({@link Port}, {@link PrimaryPort}, {@link SecondaryPort})
     * não dependem de adaptadores;</li>
     * <li>adaptadores primários não dependem de adaptadores secundários, e vice-versa. Um {@link Adapter} genérico
     * (sem lado definido) fica isento desta segunda verificação.</li>
     * </ul>
     */
    public static ArchRule hexagonal() {
        return ArchRuleDefinition.classes()
                .should(new HexagonalCondition())
                .allowEmptyShould(true)
                .as("Arquitetura hexagonal: núcleo não depende de adaptadores e adaptadores primários e secundários "
                        + "não dependem uns dos outros");
    }

    private static ArchRule orderedRule(String name, List<Class<? extends Annotation>> levels, boolean strict,
                                        String because) {
        return ArchRuleDefinition.classes()
                .should(new OrderedLevelsCondition(levels, strict))
                .allowEmptyShould(true)
                .as(name + ": " + because);
    }

    /**
     * Resolve a anotação mais próxima de um tipo dentre as candidatas, na ordem: o próprio tipo e depois a cadeia de
     * pacotes. Num mesmo elemento, vence a primeira candidata da lista.
     */
    static final class Resolver {

        private final List<Class<? extends Annotation>> candidates;
        private final Map<String, Optional<Class<? extends Annotation>>> packageCache = new ConcurrentHashMap<>();

        Resolver(List<Class<? extends Annotation>> candidates) {
            this.candidates = candidates;
        }

        Optional<Class<? extends Annotation>> resolve(JavaClass type) {
            Optional<Class<? extends Annotation>> own = match(type);
            if (own.isPresent()) {
                return own;
            }
            return resolve(type.getPackage());
        }

        private Optional<Class<? extends Annotation>> resolve(JavaPackage pkg) {
            Optional<Class<? extends Annotation>> cached = packageCache.get(pkg.getName());
            if (cached != null) {
                return cached;
            }
            Optional<Class<? extends Annotation>> result = match(pkg);
            if (result.isEmpty()) {
                result = pkg.getParent().flatMap(this::resolve);
            }
            packageCache.put(pkg.getName(), result);
            return result;
        }

        private Optional<Class<? extends Annotation>> match(CanBeAnnotated element) {
            for (Class<? extends Annotation> candidate : candidates) {
                if (element.isAnnotatedWith(candidate) || element.isMetaAnnotatedWith(candidate)) {
                    return Optional.of(candidate);
                }
            }
            return Optional.empty();
        }
    }

    private static final class OrderedLevelsCondition extends ArchCondition<JavaClass> {

        private final List<Class<? extends Annotation>> levels;
        private final boolean strict;
        private final Resolver resolver;

        OrderedLevelsCondition(List<Class<? extends Annotation>> levels, boolean strict) {
            super("respeitar a ordem " + names(levels));
            this.levels = levels;
            this.strict = strict;
            this.resolver = new Resolver(levels);
        }

        @Override
        public void check(JavaClass origin, ConditionEvents events) {
            Optional<Class<? extends Annotation>> originLevel = resolver.resolve(origin);
            if (originLevel.isEmpty()) {
                return;
            }
            int from = levels.indexOf(originLevel.get());
            for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                if (target.equals(origin)) {
                    continue;
                }
                Optional<Class<? extends Annotation>> targetLevel = resolver.resolve(target);
                if (targetLevel.isEmpty()) {
                    continue;
                }
                int to = levels.indexOf(targetLevel.get());
                boolean violated = to < from || (strict && to > from + 1);
                if (violated) {
                    events.add(SimpleConditionEvent.violated(dependency, String.format(
                            "%s (@%s) não pode depender de %s (@%s): %s",
                            origin.getName(), originLevel.get().getSimpleName(),
                            target.getName(), targetLevel.get().getSimpleName(),
                            dependency.getDescription())));
                }
            }
        }
    }

    private static final class HexagonalCondition extends ArchCondition<JavaClass> {

        private final Resolver resolver = new Resolver(HEXAGONAL);

        HexagonalCondition() {
            super("respeitar as fronteiras da arquitetura hexagonal");
        }

        @Override
        public void check(JavaClass origin, ConditionEvents events) {
            Optional<Class<? extends Annotation>> originRole = resolver.resolve(origin);
            if (originRole.isEmpty()) {
                return;
            }
            for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                if (target.equals(origin)) {
                    continue;
                }
                Optional<Class<? extends Annotation>> targetRole = resolver.resolve(target);
                if (targetRole.isEmpty()) {
                    continue;
                }
                String reason = violation(originRole.get(), targetRole.get());
                if (reason != null) {
                    events.add(SimpleConditionEvent.violated(dependency, String.format(
                            "%s (@%s) não pode depender de %s (@%s) — %s: %s",
                            origin.getName(), originRole.get().getSimpleName(),
                            target.getName(), targetRole.get().getSimpleName(),
                            reason, dependency.getDescription())));
                }
            }
        }

        private static String violation(Class<? extends Annotation> from, Class<? extends Annotation> to) {
            boolean fromCore = !isAdapter(from);
            if (fromCore && isAdapter(to)) {
                return "o núcleo (aplicação e portas) não depende de adaptadores";
            }
            if (from == PrimaryAdapter.class && to == SecondaryAdapter.class) {
                return "adaptador primário não depende de adaptador secundário";
            }
            if (from == SecondaryAdapter.class && to == PrimaryAdapter.class) {
                return "adaptador secundário não depende de adaptador primário";
            }
            return null;
        }

        private static boolean isAdapter(Class<? extends Annotation> role) {
            return role == Adapter.class || role == PrimaryAdapter.class || role == SecondaryAdapter.class;
        }
    }

    private static String names(List<Class<? extends Annotation>> levels) {
        StringBuilder sb = new StringBuilder();
        for (Class<? extends Annotation> level : levels) {
            if (!sb.isEmpty()) {
                sb.append(" -> ");
            }
            sb.append('@').append(level.getSimpleName());
        }
        return sb.toString();
    }
}
