package br.com.archbase.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/** Até a 3.5.0 o Spring recusava {@code @ArchbaseTest} (aliases com padrões diferentes) e nenhum teste rodava. */
@ArchbaseTest(classes = ArchbaseTestUsavelTest.Config.class, properties = "teste.chave=valor")
@DisplayName("@ArchbaseTest: padrão")
class ArchbaseTestUsavelTest {

    @Configuration
    static class Config {
    }

    @Autowired
    Environment environment;

    @Value("${teste.chave}")
    String chave;

    @Test
    @DisplayName("sobe o contexto com o perfil test e as propriedades")
    void sobe() {
        assertThat(environment.getActiveProfiles()).containsExactly("test");
        assertThat(chave).isEqualTo("valor");
    }
}
