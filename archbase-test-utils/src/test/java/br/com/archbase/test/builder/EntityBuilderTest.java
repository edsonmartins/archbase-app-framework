package br.com.archbase.test.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EntityBuilder.withDefaults")
class EntityBuilderTest {

    /** Como PersistenceEntityBase: id String, versão Long. */
    public static class ComIdTexto {
        private String id;
        private String code;
        private Long version;
        private boolean active;

        public ComIdTexto() {
        }
    }

    public static class ComIdUuid {
        private UUID id;
        private int version;

        public ComIdUuid() {
        }
    }

    public static class SemConstrutorPadrao {
        public SemConstrutorPadrao(String obrigatorio) {
        }
    }

    @Test
    @DisplayName("id String recebe um UUID como texto (antes, build() falhava)")
    void idTexto() {
        ComIdTexto e = EntityBuilder.withDefaults(ComIdTexto.class).build();
        assertThat(UUID.fromString(e.id)).isNotNull();
        assertThat(e.code).startsWith("DEFAULT-");
        assertThat(e.version).isZero();
        assertThat(e.active).isTrue();
    }

    @Test
    @DisplayName("id UUID recebe UUID; campo de tipo incompatível fica como está")
    void idUuid() {
        ComIdUuid e = EntityBuilder.withDefaults(ComIdUuid.class).build();
        assertThat(e.id).isNotNull();
        assertThat(e.version).isZero();
    }

    @Test
    @DisplayName("sem construtor padrão, a mensagem aponta o método que existe")
    void semConstrutor() {
        assertThatThrownBy(() -> EntityBuilder.withDefaults(SemConstrutorPadrao.class).build())
                .hasMessageContaining("build(Supplier)");
    }
}
