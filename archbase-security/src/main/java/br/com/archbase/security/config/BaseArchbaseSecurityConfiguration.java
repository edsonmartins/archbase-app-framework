package br.com.archbase.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Slf4j
public abstract class BaseArchbaseSecurityConfiguration implements ArchbaseSecurityConfigurator {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        configure(http);
        return http.build();
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        List<String> whiteListUrls = getWhiteListUrls();
        String[] patterns = whiteListUrls.toArray(String[]::new);
        List<String> protectedPaths = getProtectedPathsWithinWhitelist();

        logAnonymousSurface(whiteListUrls, protectedPaths);

        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> {
                    // ANTES do permitAll: no Spring Security vence a primeira regra que casa, então
                    // exigir autenticação aqui é o único jeito de proteger um caminho que está
                    // dentro de um padrão liberado (/api/v1/auth/register sob /api/v1/auth/**).
                    if (!protectedPaths.isEmpty()) {
                        auth.requestMatchers(protectedPaths.toArray(String[]::new)).authenticated();
                    }
                    auth.requestMatchers(patterns).permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    try {
                        configureAuthorizationRules(http);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(getJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // Erros de acesso: 401 quando não há autenticação válida (token ausente/expirado),
                // 403 quando há mas falta permissão. Sem o entry point o Spring devolvia 403 para
                // os dois — e clientes que renovam a sessão em 401 (o interceptor do
                // archbase-flutter, por exemplo) nunca disparavam o refresh.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(getAuthenticationEntryPoint())
                        .accessDeniedHandler(getAccessDeniedHandler()));
    }

    /**
     * Caminhos que devem exigir autenticação <b>mesmo estando</b> dentro de um padrão liberado.
     *
     * <p>Existe porque a whitelist trabalha com curingas: {@code /api/v1/auth/**} precisa ser
     * anônimo para login e reset, mas {@code /api/v1/auth/register} nem sempre deveria estar junto.
     * Sem este gancho, a única alternativa seria enumerar cada endpoint de auth.
     *
     * <p>Padrão vazio — nenhuma subtração.
     */
    protected List<String> getProtectedPathsWithinWhitelist() {
        return List.of();
    }

    /**
     * Registra, no startup, exatamente o que fica acessível sem autenticação.
     *
     * <p>A whitelist é montada em vários pontos (defaults do framework + propriedade da aplicação)
     * e ninguém costuma ver o resultado somado — foi assim que {@code /actuator/**} e rotas de uma
     * aplicação específica passaram despercebidos como públicos em todo backend. Uma linha no log
     * torna a superfície anônima verificável a cada subida.
     */
    private void logAnonymousSurface(List<String> whiteListUrls, List<String> protectedPaths) {
        log.info("Superfície anônima da API ({} padrões liberados sem autenticação): {}",
                whiteListUrls.size(), whiteListUrls);
        if (!protectedPaths.isEmpty()) {
            log.info("Exceções que voltam a exigir autenticação: {}", protectedPaths);
        }
        whiteListUrls.stream()
                .filter(url -> url.startsWith("/actuator"))
                .findAny()
                .ifPresent(url -> log.warn("'{}' está liberado sem autenticação. Se a aplicação expõe "
                        + "endpoints além de health/info (management.endpoints.web.exposure.include), "
                        + "env/configprops/heapdump ficam públicos.", url));
    }

    protected abstract CustomAccessDeniedHandler getAccessDeniedHandler();

    /**
     * Entry point para requisições não autenticadas. Default: {@link CustomAuthenticationEntryPoint}
     * (401). Sobrescreva apenas se a aplicação precisar de outro corpo/comportamento.
     */
    protected CustomAuthenticationEntryPoint getAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint();
    }

    // Adicione estes métodos abstratos para CORS
    protected abstract List<String> getAllowedOrigins();
    protected abstract List<String> getAllowedMethods();
    protected abstract List<String> getAllowedHeaders();
    protected abstract boolean getAllowCredentials();

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(getAllowedOrigins());
        configuration.setAllowedMethods(getAllowedMethods());
        configuration.setAllowedHeaders(getAllowedHeaders());
        configuration.setAllowCredentials(getAllowCredentials());
        configuration.setExposedHeaders(List.of("Authorization")); // Headers expostos

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    protected abstract List<String> getWhiteListUrls();

    protected abstract void configureAuthorizationRules(HttpSecurity http) throws Exception;

    protected abstract ArchbaseJwtAuthenticationFilter getJwtAuthenticationFilter();

}