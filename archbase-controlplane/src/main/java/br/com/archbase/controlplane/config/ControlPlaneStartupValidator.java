package br.com.archbase.controlplane.config;

import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Não deixa a aplicação subir com o módulo ligado e mal configurado. Lista todos os problemas de uma vez.
 * <p>
 * Além das propriedades do módulo, exige {@code archbase.app.tenant.fail-on-missing=true}: com o módulo ligado,
 * nenhum caminho pode cair no tenant padrão, e o resolver do Hibernate (archbase-multitenancy) cai nele quando o
 * contexto está vazio, a menos que essa chave esteja ligada.
 *
 * @since 3.4.6
 */
public class ControlPlaneStartupValidator {

    public ControlPlaneStartupValidator(ControlPlaneProperties properties, Environment environment) {
        List<String> problems = new ArrayList<>();
        if (isBlank(properties.effectiveIssuer())) {
            problems.add("archbase.controlplane.url (ou archbase.controlplane.issuer) não definida");
        }
        if (isBlank(properties.getPublicUrl())) {
            problems.add("archbase.controlplane.public-url não definida: é ela que o claim aud tem de conter");
        }
        if (isBlank(properties.getProduct())) {
            problems.add("archbase.controlplane.product não definido");
        }
        if (isBlank(properties.getJwks().getUrl()) && isBlank(properties.getJwks().getLocalPath())) {
            problems.add("defina archbase.controlplane.jwks.url e/ou archbase.controlplane.jwks.local-path");
        }
        if (!environment.getProperty("archbase.app.tenant.fail-on-missing", Boolean.class, false)) {
            problems.add("archbase.app.tenant.fail-on-missing precisa ser true com o Control Plane ligado: "
                    + "sem isso, consulta sem tenant no contexto cai em archbase.app.tenant.default.id");
        }
        if (properties.mqttEnabled()) {
            if (isBlank(properties.getInstanceId())) {
                problems.add("archbase.controlplane.instance-id é obrigatório com o MQTT ligado (mqtt.uri)");
            }
            if (isBlank(properties.getInstanceCredential()) && isBlank(properties.getMqtt().getPassword())) {
                problems.add("archbase.controlplane.instance-credential (ou mqtt.password) é obrigatório com o MQTT "
                        + "ligado");
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("archbase-controlplane ligado com configuração incompleta:\n - "
                    + String.join("\n - ", problems));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
