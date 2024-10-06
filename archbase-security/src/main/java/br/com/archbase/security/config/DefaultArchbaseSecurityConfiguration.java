package br.com.archbase.security.config;


import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class DefaultArchbaseSecurityConfiguration extends BaseArchbaseSecurityConfiguration {

    @Value("${archbase.security.whitelist}")
    private String whitelist;

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
                "/v2/externalapi-docs",
                "/v3/externalapi-docs",
                "/v3/api-docs",
                "/v3/api-docs.yaml",
                "/v3/api-docs/swagger-config",
                "/swagger-resources/**",
                "/swagger-ui.html",
                "/swagger-ui/index.html",
                "/webjars/**",
                "/actuator/**"
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
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
    }

    @Override
    protected ArchbaseJwtAuthenticationFilter getJwtAuthenticationFilter() {
        return jwtAuthenticationFilter;
    }

}