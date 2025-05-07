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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnMissingBean(BaseArchbaseSecurityConfiguration.class)
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

    @PostConstruct
    public void init() {
        finalWhitelist = Lists.newArrayList(
                "/api/v1/auth/**","/api/v1/apiToken/activate",
                "/api/v1/assistente-virtual/webhook",
                // Swagger UI v3 (OpenAPI)
                "/v3/api-docs/**",
                "/v3/api-docs.yaml",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/swagger-resources/**",
                "/webjars/**",
                // Swagger UI v2
                "/v2/api-docs/**",
                "/configuration/ui",
                "/configuration/security",
                "/swagger-resources/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/webjars/**",
                // Outros caminhos
                "/actuator/**",
                "/api/files/**",
                "/static/**",
                "/*.html",
                "/*.png",
                "/*.jpeg",
                "/*.jpg",
                "/api/v1/licenca/verificar-tenants/**"
        );
        if (!whitelist.isEmpty()) {
            finalWhitelist.addAll(Arrays.stream(whitelist.split(",")).toList());
        }
    }

    @Override
    protected List<String> getWhiteListUrls() {
        return finalWhitelist;
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