package br.com.archbase.oidc.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Leitura de papéis dos claims")
class OidcRoleSynchronizerTest {

    @Test
    @DisplayName("texto, lista de texto, caminho aninhado, lista de objetos com name (ArchGuard) e nome-URL (Auth0)")
    void formatos() {
        Map<String, Object> claims = Map.of(
                "scp", "leitura",
                "groups", List.of("a", "b"),
                "realm_access", Map.of("roles", List.of("c")),
                "roles", List.of(Map.of("name", "d", "owner", "org"), Map.of("owner", "sem-nome")),
                "https://empresa.com/roles", List.of("e"));

        assertThat(OidcRoleSynchronizer.externalRoles(claims,
                List.of("scp", "groups", "realm_access.roles", "roles", "https://empresa.com/roles", "ausente")))
                .containsExactly("leitura", "a", "b", "c", "d", "e");
    }
}
