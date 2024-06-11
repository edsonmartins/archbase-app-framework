package br.com.archbase.security.config;


import com.google.common.collect.Lists;
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

    @Value("${archbase.security.whitelist:}")
    private String whitelist;

    private final ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter;

    public DefaultArchbaseSecurityConfiguration(ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }


    @Override
    protected List<String> getWhiteListUrls() {
        List<String> defaultWhitelist = Lists.newArrayList(
                "/api/v1/auth/**",
                "/v2/externalapi-docs",
                "/v3/externalapi-docs",
                "/swagger-resources/**",
                "/swagger-ui.html",
                "/webjars/**"
        );
        if (!whitelist.isEmpty()) {
            defaultWhitelist.addAll(Arrays.stream(whitelist.split(",")).toList());
        }
        return defaultWhitelist;
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