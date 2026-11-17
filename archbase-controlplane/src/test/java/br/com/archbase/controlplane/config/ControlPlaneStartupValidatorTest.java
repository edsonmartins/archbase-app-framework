package br.com.archbase.controlplane.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControlPlaneStartupValidatorTest {

    @Test
    void listaTodosOsProblemasDeUmaVez() {
        assertThatThrownBy(() -> new ControlPlaneStartupValidator(new ControlPlaneProperties(), new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .contains("archbase.controlplane.url")
                        .contains("public-url")
                        .contains("product")
                        .contains("jwks")
                        .contains("fail-on-missing"));
    }

    @Test
    void configuracaoCompletaPassa() {
        ControlPlaneProperties p = new ControlPlaneProperties();
        p.setUrl("https://cp");
        p.setPublicUrl("https://instancia");
        p.setProduct("powerview");
        p.getJwks().setLocalPath("/tmp/jwks.json");
        MockEnvironment env = new MockEnvironment().withProperty("archbase.app.tenant.fail-on-missing", "true");

        assertThatCode(() -> new ControlPlaneStartupValidator(p, env)).doesNotThrowAnyException();
    }

    @Test
    void issuerExplicitoTemPrecedencia() {
        ControlPlaneProperties p = new ControlPlaneProperties();
        p.setUrl("https://cp");
        assertThat(p.effectiveIssuer()).isEqualTo("https://cp");
        p.setIssuer("https://emissor");
        assertThat(p.effectiveIssuer()).isEqualTo("https://emissor");
    }
}
