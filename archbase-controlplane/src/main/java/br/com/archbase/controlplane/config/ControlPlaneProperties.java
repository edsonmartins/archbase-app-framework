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

    private final Mqtt mqtt = new Mqtt();

    private final Heartbeat heartbeat = new Heartbeat();

    /**
     * Versão do produto, enviada no heartbeat. Mudou desde a última publicação, os presets e o manifesto de features
     * são publicados de novo.
     */
    private String productVersion;

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

    /** Conexão com o broker do Control Plane. Sem {@code uri}, o canal MQTT fica desligado. */
    public static class Mqtt {

        /** Broker: {@code ssl://host:8883} ou {@code wss://host:443/mqtt}. */
        private String uri;

        /** Usuário no broker. Vazio: o {@code instance-id}. */
        private String username;

        /** Senha no broker. Vazia: a {@code instance-credential}. */
        private String password;

        /** Client id. Vazio: o {@code instance-id} — estável, para a sessão persistente ser reencontrada. */
        private String clientId;

        /** Quanto tempo o broker guarda a sessão (e os comandos pendentes) com a instância desconectada. */
        private java.time.Duration sessionExpiry = java.time.Duration.ofDays(7);

        /** Espera inicial e máxima entre tentativas de reconexão (crescente). */
        private java.time.Duration reconnectMinDelay = java.time.Duration.ofSeconds(1);
        private java.time.Duration reconnectMaxDelay = java.time.Duration.ofMinutes(2);

        private java.time.Duration keepAlive = java.time.Duration.ofSeconds(60);

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public java.time.Duration getSessionExpiry() {
            return sessionExpiry;
        }

        public void setSessionExpiry(java.time.Duration sessionExpiry) {
            this.sessionExpiry = sessionExpiry;
        }

        public java.time.Duration getReconnectMinDelay() {
            return reconnectMinDelay;
        }

        public void setReconnectMinDelay(java.time.Duration reconnectMinDelay) {
            this.reconnectMinDelay = reconnectMinDelay;
        }

        public java.time.Duration getReconnectMaxDelay() {
            return reconnectMaxDelay;
        }

        public void setReconnectMaxDelay(java.time.Duration reconnectMaxDelay) {
            this.reconnectMaxDelay = reconnectMaxDelay;
        }

        public java.time.Duration getKeepAlive() {
            return keepAlive;
        }

        public void setKeepAlive(java.time.Duration keepAlive) {
            this.keepAlive = keepAlive;
        }
    }

    /** Heartbeat para o Control Plane: pelo MQTT e, com o broker fora do ar, por HTTPS. */
    public static class Heartbeat {

        private java.time.Duration interval = java.time.Duration.ofMinutes(5);

        /** Endpoint HTTPS do fallback. Vazio: {@code {url}/api/v1/instances/{instance-id}/heartbeat}. */
        private String url;

        public java.time.Duration getInterval() {
            return interval;
        }

        public void setInterval(java.time.Duration interval) {
            this.interval = interval;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
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

    public Mqtt getMqtt() {
        return mqtt;
    }

    public Heartbeat getHeartbeat() {
        return heartbeat;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    /** Se o canal MQTT está configurado. */
    public boolean mqttEnabled() {
        return mqtt.getUri() != null && !mqtt.getUri().isBlank();
    }

    public RestrictedMode getRestrictedMode() {
        return restrictedMode;
    }

    public Schema getSchema() {
        return schema;
    }
}
