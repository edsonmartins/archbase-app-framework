package br.com.archbase.test.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@WithMockArchbaseUser")
class WithMockArchbaseUserTest {

    @WithMockArchbaseUser(username = "ana@empresa.com", roles = {"ADMIN"}, tenantId = "t1")
    void anotado() {
    }

    @Test
    @DisplayName("getName() é o username, não o toString() do principal")
    void nome() throws Exception {
        WithMockArchbaseUser anotacao = getClass().getDeclaredMethod("anotado").getAnnotation(WithMockArchbaseUser.class);
        var autenticacao = new WithMockArchbaseUserFactory().createSecurityContext(anotacao).getAuthentication();

        assertThat(autenticacao.getName()).isEqualTo("ana@empresa.com");
        assertThat(autenticacao.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        assertThat(((WithMockArchbaseUserFactory.ArchbaseTestPrincipal) autenticacao.getPrincipal()).tenantId())
                .isEqualTo("t1");
    }
}
