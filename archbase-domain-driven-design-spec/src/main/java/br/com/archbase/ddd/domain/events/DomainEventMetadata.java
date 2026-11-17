package br.com.archbase.ddd.domain.events;

import br.com.archbase.ddd.domain.annotations.DomainEvent;
import br.com.archbase.ddd.domain.annotations.Externalized;

import java.util.Optional;

/**
 * Lê os metadados declarados em {@link DomainEvent} e {@link Externalized}, com os padrões documentados nelas.
 * <p>
 * O nome lógico ({@link #qualifiedName(Class)}) é o que deve ir para fora da aplicação (tipo no outbox, cabeçalho no
 * broker): ele não muda quando a classe é renomeada ou movida, desde que {@code namespace} e {@code name} estejam
 * declarados.
 *
 * @since 3.4.6
 */
public final class DomainEventMetadata {

    private DomainEventMetadata() {
    }

    /** {@link DomainEvent#namespace()}, ou o pacote do tipo quando vazio ou ausente. */
    public static String namespace(Class<?> eventType) {
        DomainEvent annotation = eventType.getAnnotation(DomainEvent.class);
        if (annotation != null && !annotation.namespace().isBlank()) {
            return annotation.namespace();
        }
        return eventType.getPackageName();
    }

    /** {@link DomainEvent#name()}, ou o nome simples do tipo quando vazio ou ausente. */
    public static String name(Class<?> eventType) {
        DomainEvent annotation = eventType.getAnnotation(DomainEvent.class);
        if (annotation != null && !annotation.name().isBlank()) {
            return annotation.name();
        }
        return eventType.getSimpleName();
    }

    /** {@code namespace.name}. */
    public static String qualifiedName(Class<?> eventType) {
        String namespace = namespace(eventType);
        return namespace.isEmpty() ? name(eventType) : namespace + "." + name(eventType);
    }

    /** Se o tipo está marcado com {@link Externalized}. */
    public static boolean isExternalized(Class<?> eventType) {
        return eventType.isAnnotationPresent(Externalized.class);
    }

    /**
     * Destino declarado em {@link Externalized} ({@code target} tem precedência sobre {@code value}). Vazio se o
     * evento não for externalizado; {@code Optional.of("")} se for, mas sem destino explícito — a infraestrutura
     * escolhe por convenção.
     */
    public static Optional<String> externalTarget(Class<?> eventType) {
        Externalized annotation = eventType.getAnnotation(Externalized.class);
        if (annotation == null) {
            return Optional.empty();
        }
        return Optional.of(!annotation.target().isBlank() ? annotation.target() : annotation.value());
    }
}
