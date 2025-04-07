package br.com.archbase.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.List;

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
                // Adicione o tratamento de exceção para capturar detalhes de erros de acesso
                .exceptionHandling(handling -> handling
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