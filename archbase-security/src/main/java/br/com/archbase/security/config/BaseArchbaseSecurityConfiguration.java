package br.com.archbase.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

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

        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> {
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