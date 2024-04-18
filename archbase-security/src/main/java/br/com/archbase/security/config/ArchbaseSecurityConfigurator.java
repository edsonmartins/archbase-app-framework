package br.com.archbase.security.config;


import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface ArchbaseSecurityConfigurator {
    void configure(HttpSecurity http) throws Exception;
}
