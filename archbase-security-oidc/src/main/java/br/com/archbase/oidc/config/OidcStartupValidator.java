package br.com.archbase.oidc.config;

import br.com.archbase.oidc.config.ArchbaseOidcProperties.LinkByEmail;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Confere a configuração dos provedores antes de qualquer token ser aceito. Configuração errada aqui não é
 * inconveniente: é token forjável (algoritmo simétrico com chave pública, {@code aud} sem conferência, emissor por
 * HTTP) ou tenant escolhido pelo cliente. Por isso a subida falha, listando todos os erros de uma vez.
 *
 * @since 3.5.0
 */
public final class OidcStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(OidcStartupValidator.class);

    /** Só assimétricos. HS* com a chave pública do JWKS como segredo é o ataque clássico de confusão de algoritmo. */
    static final Set<String> ALLOWED_ALGORITHMS = Set.of(
            "RS256", "RS384", "RS512", "PS256", "PS384", "PS512", "ES256", "ES384", "ES512");

    static final String TENANT_PLACEHOLDER = "{tid}";

    /** Vai para a coluna PROVIDER_ID do vínculo. */
    private static final Pattern PROVIDER_ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,39}");

    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private OidcStartupValidator() {
    }

    /** @throws IllegalStateException com todos os erros encontrados */
    public static void validate(ArchbaseOidcProperties properties) {
        List<String> errors = new ArrayList<>();
        Map<String, String> issuers = new HashMap<>();

        properties.getProviders().forEach((id, provider) -> {
            String at = "archbase.security.oidc.providers." + id;
            if (!PROVIDER_ID.matcher(id).matches()) {
                errors.add(at + ": id deve ter só minúsculas, dígitos e hífen (até 40) — ele é gravado no vínculo");
            }
            validateIssuer(at, provider, errors);
            validateTransport(at, provider, errors);
            if (provider.getAudiences() == null || provider.getAudiences().stream().allMatch(OidcStartupValidator::blank)) {
                errors.add(at + ".audiences: obrigatório — sem conferir o aud, token emitido para outra aplicação passa");
            }
            validateAlgorithms(at, provider, errors);
            validateTenant(at, provider, errors);
            validateProvisioning(at, provider, errors);
            validateRoleMapping(at, provider, errors);
            validateIntrospection(at, provider, errors);
            validateBackchannel(at, provider, errors);
            if (blank(provider.getClaims().getSubject())) {
                errors.add(at + ".claims.subject: obrigatório");
            }
            Duration skew = provider.getClockSkew();
            if (skew == null || skew.isNegative() || skew.compareTo(MAX_CLOCK_SKEW) > 0) {
                errors.add(at + ".clock-skew: entre 0 e 5 minutos");
            }
            if (!blank(provider.getIssuer())) {
                String other = issuers.put(provider.getIssuer(), id);
                if (other != null) {
                    errors.add(at + ".issuer: o mesmo emissor já está no provedor '" + other
                            + "' — o token não saberia a qual provedor pertence");
                }
            }
            warnings(at, provider);
        });

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Configuração inválida do archbase-security-oidc:\n - "
                    + String.join("\n - ", errors));
        }
    }

    private static void validateIssuer(String at, Provider provider, List<String> errors) {
        boolean exact = !blank(provider.getIssuer());
        boolean pattern = !blank(provider.getIssuerPattern());
        if (exact == pattern) {
            errors.add(at + ": informe exatamente um entre issuer e issuer-pattern");
            return;
        }
        if (pattern) {
            String p = provider.getIssuerPattern();
            if (p.indexOf(TENANT_PLACEHOLDER) < 0 || p.indexOf(TENANT_PLACEHOLDER) != p.lastIndexOf(TENANT_PLACEHOLDER)) {
                errors.add(at + ".issuer-pattern: deve conter " + TENANT_PLACEHOLDER + " exatamente uma vez");
            }
            if (blank(provider.getJwksUri())) {
                errors.add(at + ".jwks-uri: obrigatório com issuer-pattern (não há emissor único para descobrir)");
            }
            if (provider.getAllowedTenants() == null || provider.getAllowedTenants().isEmpty()) {
                errors.add(at + ".allowed-tenants: obrigatório com issuer-pattern — sem ele, qualquer tenant do IdP "
                        + "entraria");
            }
        } else if (provider.getAllowedTenants() != null && !provider.getAllowedTenants().isEmpty()) {
            errors.add(at + ".allowed-tenants: só se aplica a issuer-pattern");
        }
    }

    private static void validateTransport(String at, Provider provider, List<String> errors) {
        if (provider.isAllowInsecureHttp()) {
            return;
        }
        for (String[] url : new String[][]{
                {"issuer", provider.getIssuer()},
                {"issuer-pattern", provider.getIssuerPattern()},
                {"jwks-uri", provider.getJwksUri()},
                {"logout.end-session-uri", provider.getLogout().getEndSessionUri()}}) {
            if (!blank(url[1]) && !url[1].startsWith("https://")) {
                errors.add(at + "." + url[0] + ": precisa ser https:// (allow-insecure-http só em desenvolvimento)");
            }
        }
    }

    private static void validateAlgorithms(String at, Provider provider, List<String> errors) {
        if (provider.getAlgorithms() == null || provider.getAlgorithms().isEmpty()) {
            errors.add(at + ".algorithms: informe ao menos um (ex.: RS256)");
            return;
        }
        for (String alg : provider.getAlgorithms()) {
            if (!ALLOWED_ALGORITHMS.contains(alg)) {
                errors.add(at + ".algorithms: '" + alg + "' não é aceito — só " + ALLOWED_ALGORITHMS);
            }
        }
    }

    private static void validateTenant(String at, Provider provider, List<String> errors) {
        ArchbaseOidcProperties.Tenant tenant = provider.getTenant();
        int sources = (blank(tenant.getClaim()) ? 0 : 1) + (blank(tenant.getFixed()) ? 0 : 1)
                + (tenant.isHeader() ? 1 : 0) + (blank(provider.getIssuerPattern()) ? 0 : 1);
        if (sources != 1) {
            errors.add(at + ".tenant: informe exatamente uma fonte — claim, fixed ou header=true "
                    + "(com issuer-pattern, a fonte é allowed-tenants e nenhuma outra)");
        }
    }

    private static void validateIntrospection(String at, Provider provider, List<String> errors) {
        ArchbaseOidcProperties.Introspection in = provider.getIntrospection();
        if (!in.isEnabled()) {
            return;
        }
        if (blank(in.getUri())) {
            errors.add(at + ".introspection.uri: obrigatório com introspection.enabled=true");
        } else if (!provider.isAllowInsecureHttp() && !in.getUri().startsWith("https://")) {
            errors.add(at + ".introspection.uri: precisa ser https:// — o token e a credencial do cliente vão nela");
        }
        if (blank(in.getClientId()) || blank(in.getClientSecret())) {
            errors.add(at + ".introspection.client-id/client-secret: obrigatórios (a RFC 7662 exige o cliente "
                    + "autenticado)");
        }
        if (in.getCacheTtl() == null || in.getCacheTtl().isNegative() || in.getCacheTtl().compareTo(Duration.ofMinutes(10)) > 0) {
            errors.add(at + ".introspection.cache-ttl: entre 0 e 10 minutos — acima disso a revogação deixa de ser útil");
        }
        if (in.getOnError() == ArchbaseOidcProperties.OnError.ALLOW) {
            log.warn("{}.introspection.on-error=ALLOW: com o IdP fora do ar, token revogado continua valendo até o exp.", at);
        }
    }

    private static void validateBackchannel(String at, Provider provider, List<String> errors) {
        ArchbaseOidcProperties.Backchannel bc = provider.getLogout().getBackchannel();
        if (!bc.isEnabled()) {
            return;
        }
        if (bc.getMaxAge() == null || bc.getMaxAge().isNegative() || bc.getMaxAge().isZero()
                || bc.getMaxAge().compareTo(Duration.ofMinutes(15)) > 0) {
            errors.add(at + ".logout.backchannel.max-age: entre 1 segundo e 15 minutos — aviso velho é replay");
        }
        if (bc.getTokenLifetime() == null || bc.getTokenLifetime().compareTo(Duration.ofMinutes(1)) < 0
                || bc.getTokenLifetime().compareTo(Duration.ofDays(30)) > 0) {
            errors.add(at + ".logout.backchannel.token-lifetime: entre 1 minuto e 30 dias — precisa cobrir a vida do "
                    + "access token mais longo do IdP");
        }
        if (bc.isRevokeLocalSessions() && !"sub".equals(provider.getClaims().getSubject())) {
            log.warn("{}.logout.backchannel.revoke-local-sessions: claims.subject={} — o aviso do IdP só traz o sub, "
                    + "então as sessões locais deste provedor não podem ser encontradas e não serão revogadas.", at,
                    provider.getClaims().getSubject());
        }
    }

    private static void validateRoleMapping(String at, Provider provider, List<String> errors) {
        ArchbaseOidcProperties.RoleMapping mapping = provider.getRoleMapping();
        if (mapping.getSync() == ArchbaseOidcProperties.RoleSync.OFF) {
            return;
        }
        if (mapping.getMap() == null || mapping.getMap().isEmpty()) {
            errors.add(at + ".role-mapping.map: obrigatório com sync=" + mapping.getSync()
                    + " — só papéis mapeados explicitamente viram grupo");
        } else if (mapping.getMap().values().stream().anyMatch(OidcStartupValidator::blank)) {
            errors.add(at + ".role-mapping.map: todo papel precisa apontar para um nome de grupo");
        }
        if (provider.getClaims().getRoles() == null || provider.getClaims().getRoles().isEmpty()) {
            errors.add(at + ".claims.roles: obrigatório com role-mapping.sync=" + mapping.getSync()
                    + " — de onde ler os papéis no token");
        }
    }

    private static void validateProvisioning(String at, Provider provider, List<String> errors) {
        if (provider.getUsers().getProvisioning() == ArchbaseOidcProperties.Provisioning.JIT
                && provider.getTenant().isHeader()) {
            errors.add(at + ".users.provisioning=JIT não combina com tenant.header=true — o cliente escolheria em "
                    + "qual tenant criar o usuário");
        }
    }

    private static void warnings(String at, Provider provider) {
        if (provider.getUsers().getProvisioning() == ArchbaseOidcProperties.Provisioning.JIT
                && provider.getUsers().getJit().getStatus() == ArchbaseOidcProperties.JitStatus.ACTIVE) {
            log.warn("{}.users.jit.status=ACTIVE: usuário criado no primeiro acesso entra sem aprovação. Ele nasce "
                    + "sem permissões; o acesso real vem de grupos atribuídos ou do mapeamento de papéis.", at);
        }
        if (provider.getUsers().getLinkByEmail() == LinkByEmail.TRUSTED_PROVIDER) {
            log.warn("{}.users.link-by-email=TRUSTED_PROVIDER: o primeiro acesso vincula pelo e-mail mesmo sem "
                    + "e-mail verificado. Só use se o IdP não deixar o usuário escolher um e-mail alheio.", at);
        }
        if (provider.getTenant().isHeader() && provider.getUsers().getLinkByEmail() != LinkByEmail.NEVER) {
            log.info("{}: tenant pelo X-TENANT-ID — o vínculo automático por e-mail fica desligado; só entra quem já "
                    + "tem vínculo naquele tenant.", at);
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
