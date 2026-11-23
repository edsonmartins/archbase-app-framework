package br.com.archbase.oidc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code archbase.security.oidc}: IdPs OIDC cujos access tokens a aplicação aceita, ao lado do login próprio.
 * <p>
 * Só é lido se o jar {@code archbase-security-oidc} estiver no classpath. Sem provedor configurado, o módulo fica
 * inerte. Exemplos por IdP em {@code archbase-security/PROPOSTA_IDP_EXTERNO_OIDC.md} §7.
 *
 * @since 3.5.0
 */
@ConfigurationProperties(prefix = "archbase.security.oidc")
public class ArchbaseOidcProperties {

    /** Provedores por id (o id é gravado no vínculo do usuário — não o troque depois de em uso). */
    private Map<String, Provider> providers = new LinkedHashMap<>();

    private final Exchange exchange = new Exchange();

    public Exchange getExchange() {
        return exchange;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }

    public static class Provider {

        /** Emissor exato ({@code iss}). Use este ou {@link #issuerPattern}. */
        private String issuer;

        /**
         * Emissor por modelo, com {@code {tid}} no lugar do tenant do IdP — ex.:
         * {@code https://login.microsoftonline.com/{tid}/v2.0} (Entra ID multi-tenant). Exige {@link #jwksUri} e
         * {@link #allowedTenants}.
         */
        private String issuerPattern;

        /** Tenant do IdP ({@code {tid}}) → tenant do Archbase. Tenant do IdP fora do mapa é recusado. */
        private Map<String, String> allowedTenants = new LinkedHashMap<>();

        /** JWKS. Vazio = descoberto em {@code <issuer>/.well-known/openid-configuration} no primeiro uso. */
        private String jwksUri;

        /** Ao menos um precisa estar no {@code aud} do token. Obrigatório. */
        private List<String> audiences = new ArrayList<>();

        /** Algoritmos aceitos. Só assimétricos (RS*, PS*, ES*, EdDSA). */
        private List<String> algorithms = new ArrayList<>(List.of("RS256"));

        private Duration clockSkew = Duration.ofSeconds(30);

        /** Claims com valor exigido — ex.: {@code tokenType: access-token} (ArchGuard), {@code ver: "2.0"} (Entra). */
        private Map<String, String> requiredClaims = new LinkedHashMap<>();

        /** Permite {@code http://} no emissor e no JWKS. Só para desenvolvimento e teste. */
        private boolean allowInsecureHttp = false;

        private final Claims claims = new Claims();
        private final Tenant tenant = new Tenant();
        private final Users users = new Users();

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getIssuerPattern() {
            return issuerPattern;
        }

        public void setIssuerPattern(String issuerPattern) {
            this.issuerPattern = issuerPattern;
        }

        public Map<String, String> getAllowedTenants() {
            return allowedTenants;
        }

        public void setAllowedTenants(Map<String, String> allowedTenants) {
            this.allowedTenants = allowedTenants;
        }

        public String getJwksUri() {
            return jwksUri;
        }

        public void setJwksUri(String jwksUri) {
            this.jwksUri = jwksUri;
        }

        public List<String> getAudiences() {
            return audiences;
        }

        public void setAudiences(List<String> audiences) {
            this.audiences = audiences;
        }

        public List<String> getAlgorithms() {
            return algorithms;
        }

        public void setAlgorithms(List<String> algorithms) {
            this.algorithms = algorithms;
        }

        public Duration getClockSkew() {
            return clockSkew;
        }

        public void setClockSkew(Duration clockSkew) {
            this.clockSkew = clockSkew;
        }

        public Map<String, String> getRequiredClaims() {
            return requiredClaims;
        }

        public void setRequiredClaims(Map<String, String> requiredClaims) {
            this.requiredClaims = requiredClaims;
        }

        public boolean isAllowInsecureHttp() {
            return allowInsecureHttp;
        }

        public void setAllowInsecureHttp(boolean allowInsecureHttp) {
            this.allowInsecureHttp = allowInsecureHttp;
        }

        public Claims getClaims() {
            return claims;
        }

        public Tenant getTenant() {
            return tenant;
        }

        public Users getUsers() {
            return users;
        }
    }

    /**
     * Onde ler cada dado no token. Cada valor é um nome de claim (inclusive URL, como os claims com namespace do
     * Auth0) ou um caminho com pontos ({@code realm_access.roles}); o nome exato tem precedência sobre o caminho.
     */
    public static class Claims {
        /** Identificador estável do usuário. Entra ID: {@code oid}; Okta: {@code uid}. */
        private String subject = "sub";
        private String email = "email";
        /** Claim booleano de e-mail verificado; vazio = o IdP não informa. */
        private String emailVerified = "email_verified";
        private String name = "name";

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getEmailVerified() {
            return emailVerified;
        }

        public void setEmailVerified(String emailVerified) {
            this.emailVerified = emailVerified;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * De onde vem o tenant do Archbase. Exatamente uma fonte: {@code claim}, {@code fixed}, {@code header=true} ou o
     * mapa {@code allowed-tenants} do emissor por modelo.
     */
    public static class Tenant {
        /** Claim (ou caminho) com o tenant — ex.: {@code owner} (ArchGuard), {@code org_id} (Auth0). */
        private String claim;
        /** Tenant fixo — ex.: um realm do Keycloak por cliente. */
        private String fixed;
        /**
         * Tenant do {@code X-TENANT-ID}. Só vale com vínculo já existente para aquele tenant: o header não pode
         * escolher um tenant ao qual o usuário não pertence, então o vínculo automático por e-mail fica desligado.
         */
        private boolean header = false;

        public String getClaim() {
            return claim;
        }

        public void setClaim(String claim) {
            this.claim = claim;
        }

        public String getFixed() {
            return fixed;
        }

        public void setFixed(String fixed) {
            this.fixed = fixed;
        }

        public boolean isHeader() {
            return header;
        }

        public void setHeader(boolean header) {
            this.header = header;
        }
    }

    public static class Users {
        private LinkByEmail linkByEmail = LinkByEmail.VERIFIED_ONLY;
        private LocalPassword localPassword = LocalPassword.KEEP;

        public LinkByEmail getLinkByEmail() {
            return linkByEmail;
        }

        public void setLinkByEmail(LinkByEmail linkByEmail) {
            this.linkByEmail = linkByEmail;
        }

        public LocalPassword getLocalPassword() {
            return localPassword;
        }

        public void setLocalPassword(LocalPassword localPassword) {
            this.localPassword = localPassword;
        }
    }

    /**
     * Troca de token: {@code POST /api/v1/auth/oidc/exchange} recebe o access token do IdP e devolve uma sessão local
     * do Archbase — para frontends que ainda dependem do JWT próprio. Desligada por padrão.
     */
    public static class Exchange {
        private boolean enabled = false;
        /** Validade do token local emitido. Nunca passa da de {@code archbase.security.jwt.token-expiration}. */
        private Duration localTokenTtl = Duration.ofMinutes(15);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getLocalTokenTtl() {
            return localTokenTtl;
        }

        public void setLocalTokenTtl(Duration localTokenTtl) {
            this.localTokenTtl = localTokenTtl;
        }
    }

    /** Como o primeiro acesso encontra o usuário local quando ainda não há vínculo. */
    public enum LinkByEmail {
        /** Pelo e-mail, só se o token disser que ele foi verificado. Padrão. */
        VERIFIED_ONLY,
        /** Pelo e-mail sempre: o operador declara que o IdP só tem e-mails confiáveis. Gera WARN na subida. */
        TRUSTED_PROVIDER,
        /** Nunca: só entra quem já tem vínculo (criado pelo administrador ou por migração). */
        NEVER
    }

    /** O que acontece com a senha local de quem está vinculado a este provedor. */
    public enum LocalPassword {
        /** Continua valendo — quem já entrava com senha continua entrando. Padrão. */
        KEEP,
        /** Deixa de valer: login, troca e redefinição de senha local são recusados para quem tem vínculo. */
        DISABLE_ON_LINK
    }
}
