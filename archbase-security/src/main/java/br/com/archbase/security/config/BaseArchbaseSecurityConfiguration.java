package br.com.archbase.security.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
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
        AntPathRequestMatcher[] matchers = whiteListUrls.stream()
                .map(AntPathRequestMatcher::new)
                .toArray(AntPathRequestMatcher[]::new);

        http.csrf().disable()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(matchers).permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    try {
                        configureAuthorizationRules(http);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(getJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // Tratamento de exceção com semântica HTTP correta:
                // - authenticationEntryPoint → 401 quando NÃO há autenticação (token ausente,
                //   inválido, expirado ou REVOGADO). Antes, sem entry point, o Spring caía no fluxo
                //   de acesso-negado e devolvia 403 para token revogado — o que impedia o cliente de
                //   distinguir "faça refresh/login" (401) de "sem permissão" (403).
                // - accessDeniedHandler → 403 quando HÁ autenticação mas falta permissão.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                        "Não autenticado: token ausente, inválido ou revogado"))
                        .accessDeniedHandler(getAccessDeniedHandler()));
    }

    protected abstract CustomAccessDeniedHandler getAccessDeniedHandler();

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