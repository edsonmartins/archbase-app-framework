package br.com.archbase.security.config;


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

    private final ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter;

    public DefaultArchbaseSecurityConfiguration(ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }


    @Override
    protected List<String> getWhiteListUrls() {
        return Arrays.asList(
                "/api/v1/auth/**",
                "/v2/externalapi-docs",
                "/v3/externalapi-docs",
                "/v3/api-docs",
                "/v3/api-docs.yaml",
                "/v3/api-docs/swagger-config",
                "/swagger-resources/**",
                "/swagger-ui.html",
                "/swagger-ui/index.html",
                "/webjars/**"
        );
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