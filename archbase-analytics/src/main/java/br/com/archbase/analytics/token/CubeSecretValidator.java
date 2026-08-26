package br.com.archbase.analytics.token;

import br.com.archbase.analytics.proxy.AnalyticsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Confere, na subida, se o segredo do analytics serve para assinar.
 *
 * <p>Existe porque a intenção estava só no javadoc. {@code AnalyticsProperties.secret}
 * pedia "≥ 32 bytes" e nascia string vazia, e nada checava: ligar
 * {@code archbase.analytics.enabled=true} sem configurar o segredo entregava um proxy
 * que assina HS256 com chave vazia.
 *
 * <p><b>Por que isso importa.</b> O token cunhado pelo {@link CubeTokenMinter} carrega
 * as claims de escopo que o produto projeta — é o que impede o cliente de escolher
 * quais dados enxerga. Assinado com chave vazia ou trivial, esse token é forjável, e
 * o {@code DataScopeProvider} deixa de valer.
 *
 * <p><b>Por que o padrão apenas avisa.</b> Recusar a subida é a postura correta para
 * uma instalação nova, mas derrubaria aplicação que hoje roda — mal configurada, e
 * ainda assim rodando. O padrão {@code warn} deixa o estado real aparecer a cada
 * deploy, no mesmo espírito com que o validador de hardening registra as proteções
 * inertes. Quem leva o analytics a sério vira para {@code fail}.
 */
public final class CubeSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(CubeSecretValidator.class);

    private CubeSecretValidator() {
    }

    /**
     * @throws IllegalStateException quando o segredo não serve e o modo é {@code fail}
     */
    public static void validar(AnalyticsProperties props) {
        if (!props.isEnabled()) {
            return;
        }
        String modo = props.getSecretValidation() == null
                ? "warn" : props.getSecretValidation().trim().toLowerCase(Locale.ROOT);
        if ("off".equals(modo)) {
            return;
        }

        String problema = problema(props);
        if (problema == null) {
            return;
        }

        String mensagem = "[archbase-analytics] " + problema + " O proxy assina o token de escopo "
                + "com esse segredo, e é ele que impede o cliente de escolher os próprios dados: "
                + "com segredo fraco o token é forjável e a projeção do DataScopeProvider deixa de "
                + "valer. Configure archbase.analytics.secret com o mesmo valor que o Cube usa "
                + "(CUBEJS_API_SECRET), com pelo menos " + props.getSecretMinBytes() + " bytes. "
                + "Para recusar a subida nesse estado, use archbase.analytics.secret-validation=fail.";

        if ("fail".equals(modo)) {
            throw new IllegalStateException(mensagem);
        }
        log.warn(mensagem);
    }

    private static String problema(AnalyticsProperties props) {
        String secret = props.getSecret();
        if (secret == null || secret.isBlank()) {
            return "O analytics está ligado, mas archbase.analytics.secret não foi configurado.";
        }
        int bytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < props.getSecretMinBytes()) {
            return "O segredo do analytics tem " + bytes + " bytes, abaixo do mínimo de "
                    + props.getSecretMinBytes() + ".";
        }
        return null;
    }
}
