package br.com.archbase.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração do {@code archbase-controlplane}. Tudo vem do ambiente: nenhum segredo tem valor padrão.
 * <p>
 * Obrigatórias com o módulo ligado: {@code url} (ou {@code issuer}), {@code public-url}, {@code product} e ao menos
 * uma fonte de chaves ({@code jwks.url} ou {@code jwks.local-path}). A falta de qualquer uma impede a subida — ver
 * {@link ControlPlaneStartupValidator}.
 *
 * @since 3.4.6
 */
@ConfigurationProperties(prefix = "archbase.controlplane")
public class ControlPlaneProperties {

    /** Liga o módulo. Desligado, a aplicação se comporta exatamente como sem ele. */
    private boolean enabled;

    /** Endereço do Control Plane. */
    private String url;

    /** Valor esperado no claim {@code iss}. Vazio: usa {@link #url}. */
    private String issuer;

    /** Código do produto no catálogo do Control Plane (ex.: {@code powerview}). */
    private String product;

    /** Identidade da instância, emitida no primeiro registro (usada a partir da Fase C). */
    private String instanceId;

    /** Credencial da instância (usada a partir da Fase C). */
    private String instanceCredential;

    /** URL pública da instância: o claim {@code aud} do token tem de contê-la. */
    private String publicUrl;

    /** Convivência com o JWT próprio do Archbase durante a migração. */
    private LegacyJwtMode legacyJwt = LegacyJwtMode.ACCEPT;

    /** Tolerância de relógio na validação de {@code exp}/{@code iat}. */
    private Duration clockSkew = Duration.ofSeconds(30);

    /** Produto com assento obrigatório: token de sessão sem {@code seat} é recusado com {@code NO_SEAT}. */
    private boolean seatRequired;

    /**
     * Carência depois do {@code exp} da licença: dentro dela a instância segue normal (status {@code GRACE}); depois,
     * entra em modo restrito. É o que segura o cliente quando o Control Plane está fora do ar.
     */
    private Duration gracePeriod = Duration.ofDays(7);

    private final License license = new License();

    private final RestrictedMode restrictedMode = new RestrictedMode();

    private final Jwks jwks = new Jwks();

    private final Schema schema = new Schema();

    public enum LegacyJwtMode {
        /** Aceita o token do Control Plane e o JWT atual; o do Control Plane tem prioridade. */
        ACCEPT,
        /** Só o token do Control Plane; o JWT atual e as rotas de login local deixam de responder. */
        REJECT
    }

    /** Chaves públicas do Control Plane. */
    public static class Jwks {

        /** Endereço do JWKS no Control Plane. */
        private String url;

        /**
         * Cópia local do JWKS: lida na subida (funciona offline) e regravada a cada busca bem-sucedida. Sem ela, a
         * instância só valida token depois de alcançar o Control Plane.
         */
        private String localPath;

        /** Intervalo mínimo entre buscas disparadas por {@code kid} desconhecido. */
        private Duration refreshMinInterval = Duration.ofSeconds(60);

        /** Timeout de conexão e leitura da busca do JWKS. */
        private Duration timeout = Duration.ofSeconds(5);

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getLocalPath() {
            return localPath;
        }

        public void setLocalPath(String localPath) {
            this.localPath = localPath;
        }

        public Duration getRefreshMinInterval() {
            return refreshMinInterval;
        }

        public void setRefreshMinInterval(Duration refreshMinInterval) {
            this.refreshMinInterval = refreshMinInterval;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    /** Tabelas próprias do módulo (vínculo de usuário; mais adiante, comandos aplicados e cache de licença). */
    public static class Schema {

        /** {@code apply}: cria na subida o que faltar; {@code off}: a aplicação cria (Flyway etc.). */
        private SchemaMode mode = SchemaMode.APPLY;

        public SchemaMode getMode() {
            return mode;
        }

        public void setMode(SchemaMode mode) {
            this.mode = mode;
        }
    }

    public enum SchemaMode {
        APPLY, OFF
    }

    /** Licença da instância. */
    public static class License {

        /**
         * Arquivo com a licença (JWS compacto). Lido na subida e aplicado se for mais recente que a guardada no banco.
         * A partir da Fase C, a licença também chega por comando do Control Plane.
         */
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /** Modo restrito: licença vencida além da carência ou tenant suspenso. */
    public static class RestrictedMode {

        /** {@code enforce}: recusa a escrita; {@code report}: só registra em log (para implantação gradual). */
        private Enforcement enforcement = Enforcement.ENFORCE;

        /**
         * Caminhos que continuam liberados no modo restrito, em padrão Ant. O login legado fica liberado: entrar para
         * ler e exportar continua permitido.
         */
        private java.util.List<String> allowedPaths = new java.util.ArrayList<>(java.util.List.of("/api/v1/auth/**"));

        public Enforcement getEnforcement() {
            return enforcement;
        }

        public void setEnforcement(Enforcement enforcement) {
            this.enforcement = enforcement;
        }

        public java.util.List<String> getAllowedPaths() {
            return allowedPaths;
        }

        public void setAllowedPaths(java.util.List<String> allowedPaths) {
            this.allowedPaths = allowedPaths;
        }
    }

    public enum Enforcement {
        ENFORCE, REPORT
    }

    /** {@link #issuer} se declarado, senão {@link #url}. */
    public String effectiveIssuer() {
        return issuer != null && !issuer.isBlank() ? issuer : url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceCredential() {
        return instanceCredential;
    }

    public void setInstanceCredential(String instanceCredential) {
        this.instanceCredential = instanceCredential;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public LegacyJwtMode getLegacyJwt() {
        return legacyJwt;
    }

    public void setLegacyJwt(LegacyJwtMode legacyJwt) {
        this.legacyJwt = legacyJwt;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    public boolean isSeatRequired() {
        return seatRequired;
    }

    public void setSeatRequired(boolean seatRequired) {
        this.seatRequired = seatRequired;
    }

    public Duration getGracePeriod() {
        return gracePeriod;
    }

    public void setGracePeriod(Duration gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public Jwks getJwks() {
        return jwks;
    }

    public License getLicense() {
        return license;
    }

    public RestrictedMode getRestrictedMode() {
        return restrictedMode;
    }

    public Schema getSchema() {
        return schema;
    }
}
