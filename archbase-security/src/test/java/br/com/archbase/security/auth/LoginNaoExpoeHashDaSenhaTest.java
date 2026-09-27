package br.com.archbase.security.auth;

import br.com.archbase.security.domain.entity.User;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resposta de login não expõe o hash da senha")
class LoginNaoExpoeHashDaSenhaTest {

    @Test
    @DisplayName("o JSON do login traz o usuário, sem o campo password")
    void semHashNoJson() {
        User user = User.builder()
                .id("u-1").userName("aluno@x.com").email("aluno@x.com")
                .password("$2a$10$hashQueNaoPodeSair")
                .build();
        AuthenticationResponse resposta = AuthenticationResponse.builder()
                .accessToken("acc").user(user).build();

        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode json = mapper.readTree(mapper.writeValueAsString(resposta));

        assertThat(json.path("user").path("email").asText()).isEqualTo("aluno@x.com");
        assertThat(json.path("user").has("password")).isFalse();
        assertThat(json.toString()).doesNotContain("hashQueNaoPodeSair");
    }
}
