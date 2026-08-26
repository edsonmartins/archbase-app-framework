package br.com.archbase.analytics;

import br.com.archbase.analytics.proxy.AnalyticsProperties;
import br.com.archbase.analytics.token.CubeSecretValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O segredo de assinatura é conferido na subida.
 *
 * <p>O que se protege aqui: o token cunhado pelo minter carrega as claims de escopo que decidem
 * quais dados o cliente enxerga. Com segredo vazio ou curto esse token é forjável, e a projeção do
 * {@code DataScopeProvider} vira decoração.
 *
 * <p>O padrão avisa em vez de recusar porque recusar derrubaria aplicação que hoje sobe mal
 * configurada — mas quem liga {@code fail} tem a garantia de não subir assim.
 */
class CubeSecretValidatorTest {

    private static final String BOM = "archbase-analytics-secret-de-teste-32bytes+";

    @Test
    @DisplayName("analytics desligado não exige segredo nenhum")
    void desligadoNaoExige() {
        assertThatCode(() -> CubeSecretValidator.validar(props(false, "", "fail")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("no padrão, segredo vazio avisa e deixa subir")
    void padraoAvisaENaoDerruba() {
        // A compatibilidade é deliberada: quem já roda assim continua rodando, com o estado real
        // aparecendo no log a cada deploy.
        assertThatCode(() -> CubeSecretValidator.validar(props(true, "", "warn")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("com fail, segredo vazio impede a subida")
    void failRecusaSegredoVazio() {
        assertThatThrownBy(() -> CubeSecretValidator.validar(props(true, "", "fail")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("archbase.analytics.secret");
    }

    @Test
    @DisplayName("com fail, segredo curto demais impede a subida")
    void failRecusaSegredoCurto() {
        assertThatThrownBy(() -> CubeSecretValidator.validar(props(true, "curto-demais", "fail")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bytes");
    }

    @Test
    @DisplayName("segredo de tamanho suficiente passa")
    void segredoBomPassa() {
        assertThatCode(() -> CubeSecretValidator.validar(props(true, BOM, "fail")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("off não confere nada, nem com segredo vazio")
    void offNaoConfere() {
        assertThatCode(() -> CubeSecretValidator.validar(props(true, "", "off")))
                .doesNotThrowAnyException();
    }

    private static AnalyticsProperties props(boolean enabled, String secret, String modo) {
        AnalyticsProperties p = new AnalyticsProperties();
        p.setEnabled(enabled);
        p.setSecret(secret);
        p.setSecretValidation(modo);
        return p;
    }
}
