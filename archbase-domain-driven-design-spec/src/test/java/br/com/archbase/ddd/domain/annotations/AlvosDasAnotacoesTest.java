package br.com.archbase.ddd.domain.annotations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Alvos de @DomainTransient e @DomainIdentifier")
class AlvosDasAnotacoesTest {

    /** Compila só com FIELD e METHOD no @Target — até a 3.5.0 não compilava. */
    static class Exemplo {
        @DomainTransient
        String calculado;

        @DomainIdentifier
        public String getId() {
            return null;
        }
    }

    @Test
    @DisplayName("as duas aceitam campo e método, além de tipo")
    void alvos() {
        for (Class<?> anotacao : List.of(DomainTransient.class, DomainIdentifier.class)) {
            var alvos = List.of(anotacao.getAnnotation(Target.class).value());
            assertTrue(alvos.containsAll(List.of(ElementType.TYPE, ElementType.FIELD, ElementType.METHOD)), anotacao + ": " + alvos);
        }
    }
}
