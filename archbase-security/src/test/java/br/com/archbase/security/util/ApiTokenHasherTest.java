package br.com.archbase.security.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiTokenHasher")
class ApiTokenHasherTest {

    @Test
    @DisplayName("é determinístico — a busca depende disso")
    void deterministico() {
        String token = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

        assertThat(ApiTokenHasher.hash(token)).isEqualTo(ApiTokenHasher.hash(token));
    }

    @Test
    @DisplayName("confere com o SHA-256 conhecido do valor")
    void valorConhecido() {
        // SHA-256 de "abc" — vetor de teste padrão do algoritmo.
        assertThat(ApiTokenHasher.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("tokens diferentes produzem hashes diferentes")
    void semColisaoTrivial() {
        assertThat(ApiTokenHasher.hash(UUID.randomUUID().toString()))
                .isNotEqualTo(ApiTokenHasher.hash(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("hash tem 64 caracteres hex minúsculos — cabe na coluna varchar(64)")
    void formatoDoHash() {
        String hash = ApiTokenHasher.hash(UUID.randomUUID().toString());

        assertThat(hash).hasSize(ApiTokenHasher.HASH_LENGTH).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("null entra, null sai — não inventa hash para token ausente")
    void nullPassaDireto() {
        assertThat(ApiTokenHasher.hash(null)).isNull();
    }

    @Test
    @DisplayName("não devolve o token em claro")
    void naoVazaOToken() {
        String token = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

        assertThat(ApiTokenHasher.hash(token)).doesNotContain(token);
    }
}
