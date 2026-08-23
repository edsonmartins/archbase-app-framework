package br.com.archbase.security.config;


import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração de segurança padrão. Esta configuração será usada automaticamente
 * a menos que uma implementação de {@link CustomSecurityConfiguration} seja fornecida.
 *
 * Para sobrepor esta configuração, crie uma classe que estenda {@link BaseArchbaseSecurityConfiguration}
 * e implemente {@link CustomSecurityConfiguration}:
 *
 * <pre>
 * @Configuration
 * @EnableWebSecurity
 * public class MySecurityConfiguration extends BaseArchbaseSecurityConfiguration
 *                                      implements CustomSecurityConfiguration {
 *     // Implementação personalizada...
 * }
 * </pre>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnMissingBean(CustomSecurityConfiguration.class)
public class DefaultArchbaseSecurityConfiguration extends BaseArchbaseSecurityConfiguration {

    @Value("${archbase.security.whitelist}")
    private String whitelist;

    @Value("${archbase.security.cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Value("${archbase.security.cors.allowed-methods}")
    private String corsAllowedMethods;

    @Value("${archbase.security.cors.allowed-headers}")
    private String corsAllowedHeaders;

    @Value("${archbase.security.cors.allow-credentials}")
    private boolean corsAllowCredentials;

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;

    private final ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter;
    private List<String> finalWhitelist;

    public DefaultArchbaseSecurityConfiguration(ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Libera {@code /actuator/**} sem autenticação. Com
     * {@code management.endpoints.web.exposure.include=*}, isto expõe {@code env},
     * {@code configprops} e {@code heapdump} — ou seja, credenciais — para qualquer um.
     */
    @Value("${archbase.security.public-paths.actuator:true}")
    private boolean publicActuator;

    /**
     * Auto-cadastro anônimo em {@code POST /api/v1/auth/register}. Continua ligado por
     * compatibilidade, mas a maioria dos backends corporativos não quer cadastro aberto — e ele
     * vinha ligado sem que ninguém tivesse escolhido isso.
     */
    @Value("${archbase.security.public-paths.registration:true}")
    private boolean publicRegistration;

    @Value("${archbase.security.public-paths.swagger:true}")
    private boolean publicSwagger;

    @Value("${archbase.security.public-paths.static-files:true}")
    private boolean publicStaticFiles;

    /**
     * Rotas de uma aplicação específica que ficaram na whitelist do framework
     * ({@code /api/v1/assistente-virtual/webhook}, {@code /api/v1/licenca/verificar-tenants/**}) e
     * hoje são públicas em <b>todo</b> backend que usa o starter, precise delas ou não. Mantidas por
     * compatibilidade; quem depende delas deve movê-las para
     * {@code archbase.security.whitelist} e desligar esta opção.
     *
     * @deprecated rotas de aplicação não pertencem ao default do framework; será removido.
     */
    @Deprecated(since = "3.0.11", forRemoval = true)
    @Value("${archbase.security.public-paths.legacy-app-routes:true}")
    private boolean publicLegacyAppRoutes;

    @PostConstruct
    public void init() {
        finalWhitelist = Lists.newArrayList(
                "/api/v1/auth/**", "/api/v1/apiToken/activate",
                // A rota de erro do container. A cadeia de segurança também filtra o dispatch
                // ERROR: sem liberá-la, o redespacho do erro é barrado e o status que chega ao
                // cliente vira 403 — inclusive para um 404 de rota inexistente ou para o 401 que o
                // entry point acabou de escrever.
                "/error"
        );

        if (publicSwagger) {
            finalWhitelist.addAll(List.of(
                    // Swagger UI v3 (OpenAPI)
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**",
                    // Swagger UI v2
                    "/v2/api-docs/**",
                    "/configuration/ui",
                    "/configuration/security"));
        }

        if (publicActuator) {
            finalWhitelist.add("/actuator/**");
        }

        if (publicStaticFiles) {
            finalWhitelist.addAll(List.of(
                    "/api/files/**",
                    "/static/**",
                    "/*.html",
                    "/*.png",
                    "/*.jpeg",
                    "/*.jpg"));
        }

        if (publicLegacyAppRoutes) {
            finalWhitelist.addAll(List.of(
                    "/api/v1/assistente-virtual/webhook",
                    "/api/v1/licenca/verificar-tenants/**"));
        }

        if (!whitelist.isEmpty()) {
            finalWhitelist.addAll(Arrays.stream(whitelist.split(",")).toList());
        }
    }

    @Override
    protected List<String> getWhiteListUrls() {
        return finalWhitelist;
    }

    @Override
    protected List<String> getProtectedPathsWithinWhitelist() {
        // /api/v1/auth/** precisa continuar anônimo (login, refresh, reset); só o cadastro sai.
        return publicRegistration ? List.of() : List.of("/api/v1/auth/register");
    }

    @Override
    protected void configureAuthorizationRules(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Libera pré-flight requests
                .anyRequest().authenticated()
        );
    }

    @Override
    protected ArchbaseJwtAuthenticationFilter getJwtAuthenticationFilter() {
        return jwtAuthenticationFilter;
    }

    @Override
    protected List<String> getAllowedOrigins() {
        return List.of(corsAllowedOrigins.split(","));
    }

    @Override
    protected List<String> getAllowedMethods() {
        return List.of(corsAllowedMethods.split(","));
    }

    @Override
    protected List<String> getAllowedHeaders() {
        return List.of(corsAllowedHeaders.split(","));
    }

    @Override
    protected boolean getAllowCredentials() {
        return corsAllowCredentials;
    }

    @Override
    protected CustomAccessDeniedHandler getAccessDeniedHandler() {
        return accessDeniedHandler;
    }

}