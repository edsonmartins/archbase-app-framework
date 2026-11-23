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
        private final RoleMapping roleMapping = new RoleMapping();
        private final Introspection introspection = new Introspection();
        private final Logout logout = new Logout();

        public Logout getLogout() {
            return logout;
        }

        public RoleMapping getRoleMapping() {
            return roleMapping;
        }

        public Introspection getIntrospection() {
            return introspection;
        }

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
        /**
         * Claims (ou caminhos) com os papéis/grupos do IdP — ex.: {@code realm_access.roles} (Keycloak), {@code roles}
         * (Entra app roles), {@code groups} (Okta), {@code permissions} (Auth0). Listas de texto, texto único, ou
         * listas de objetos com {@code name} (ArchGuard). Só são lidos com {@code role-mapping.sync} ligado.
         */
        private java.util.List<String> roles = new java.util.ArrayList<>();

        public java.util.List<String> getRoles() {
            return roles;
        }

        public void setRoles(java.util.List<String> roles) {
            this.roles = roles;
        }

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
        private Provisioning provisioning = Provisioning.EXISTING_USER;
        private LinkByEmail linkByEmail = LinkByEmail.VERIFIED_ONLY;
        private LocalPassword localPassword = LocalPassword.KEEP;
        private final Jit jit = new Jit();

        public Provisioning getProvisioning() {
            return provisioning;
        }

        public void setProvisioning(Provisioning provisioning) {
            this.provisioning = provisioning;
        }

        public Jit getJit() {
            return jit;
        }

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

    /**
     * Papéis do IdP → grupos locais. Só papéis listados em {@code map} contam; os demais são ignorados. Os cinco
     * portões continuam lendo só o banco: o papel do token vira participação em grupo, nunca permissão direta, e
     * nunca administrador.
     * <p>
     * Papel com caractere especial no nome ({@code /}, {@code :}, {@code .}) precisa de colchetes na chave — em
     * {@code .properties}: {@code role-mapping.map.[cliente-a/financeiro]=Financeiro}; em YAML:
     * {@code "[cliente-a/financeiro]": Financeiro}. Sem colchetes o Spring descarta esses caracteres da chave.
     */
    public static class RoleMapping {
        private RoleSync sync = RoleSync.OFF;
        /** Papel no IdP → nome do grupo local (no tenant do usuário). */
        private Map<String, String> map = new LinkedHashMap<>();

        public RoleSync getSync() {
            return sync;
        }

        public void setSync(RoleSync sync) {
            this.sync = sync;
        }

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }
    }

    /**
     * Introspecção (RFC 7662): pergunta ao IdP se o token ainda está ativo, para pegar revogação e logout antes do
     * {@code exp}. Feita depois de a assinatura ser validada, com cache. Disponível no ArchGuard, Keycloak e Okta;
     * Auth0 e Entra ID não oferecem.
     */
    public static class Introspection {
        private boolean enabled = false;
        /** Endpoint de introspecção do IdP. Obrigatório quando ligado. */
        private String uri;
        /** Credencial do cliente no IdP (HTTP Basic, como a RFC 7662 pede). Sem valor padrão. */
        private String clientId;
        private String clientSecret;
        /** Quanto tempo a resposta vale — nunca além do {@code exp} do token. */
        private Duration cacheTtl = Duration.ofSeconds(30);
        /** O que fazer se o IdP não responder. */
        private OnError onError = OnError.DENY;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public Duration getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
        }

        public OnError getOnError() {
            return onError;
        }

        public void setOnError(OnError onError) {
            this.onError = onError;
        }
    }

    /** Logout iniciado pela aplicação (OIDC RP-Initiated Logout). */
    public static class Logout {
        /**
         * {@code end_session_endpoint} do IdP, devolvido por {@code POST /api/v1/auth/oidc/logout} para o frontend
         * redirecionar o navegador. Vazio = o frontend usa o da própria descoberta.
         */
        private String endSessionUri;

        public String getEndSessionUri() {
            return endSessionUri;
        }

        public void setEndSessionUri(String endSessionUri) {
            this.endSessionUri = endSessionUri;
        }
    }

    public enum OnError {
        /** IdP fora do ar: recusa ({@code 503 IDP_UNAVAILABLE}). Padrão — não aceitar o que não se pode verificar. */
        DENY,
        /**
         * IdP fora do ar: aceita o token só pela assinatura. Prioriza disponibilidade sobre revogação imediata. Gera WARN
         * na subida.
         */
        ALLOW
    }

    public enum RoleSync {
        /** Papéis do token não afetam nada. Padrão. */
        OFF,
        /** Inclui o usuário nos grupos mapeados dos papéis que o token traz. Nunca remove. */
        ADD,
        /**
         * Espelha: inclui nos grupos dos papéis presentes e remove dos grupos <b>mapeados</b> cujo papel sumiu do token.
         * Grupos fora do mapa (atribuídos à mão) nunca são tocados.
         */
        MIRROR
    }

    /** O que acontece no primeiro acesso de quem não tem cadastro local. */
    public enum Provisioning {
        /** Só entra quem já tem cadastro (vinculado pelo e-mail ou pelo administrador). Padrão. */
        EXISTING_USER,
        /** Cria o usuário local no primeiro acesso ({@code just in time}), no estado de {@code users.jit.status}. */
        JIT
    }

    public static class Jit {
        private JitStatus status = JitStatus.PENDING;

        public JitStatus getStatus() {
            return status;
        }

        public void setStatus(JitStatus status) {
            this.status = status;
        }
    }

    /** Estado do usuário criado no primeiro acesso. Nos dois casos ele nasce sem grupo e sem perfil: sem permissão. */
    public enum JitStatus {
        /** Inativo até um administrador ativar. Padrão: ninguém ganha acesso sem uma decisão humana. */
        PENDING,
        /** Ativo de imediato — para IdP corporativo de um único cliente, junto com o mapeamento de papéis. */
        ACTIVE
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
