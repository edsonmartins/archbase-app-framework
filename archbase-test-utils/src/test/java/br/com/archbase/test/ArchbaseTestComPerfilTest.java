package br.com.archbase.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@ArchbaseTest(value = "integracao", classes = ArchbaseTestComPerfilTest.Config.class)
@DisplayName("@ArchbaseTest: perfil informado")
class ArchbaseTestComPerfilTest {

    @Configuration
    static class Config {
    }

    @Autowired
    Environment environment;

    @Test
    @DisplayName("value troca o perfil ativo")
    void perfil() {
        assertThat(environment.getActiveProfiles()).containsExactly("integracao");
    }
}
