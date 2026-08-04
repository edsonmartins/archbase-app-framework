package br.com.archbase.security.config;


import br.com.archbase.security.auditing.ApplicationAuditAware;
import br.com.archbase.security.crypto.ArchbaseCryptoService;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class ArchbaseSecurityApplicationConfig {

    private final UserJpaRepository repository;

    /**
     * Resolve o usuário pelo e-mail para o {@code DaoAuthenticationProvider}.
     *
     * <p><b>A exceção lançada aqui não é detalhe de estilo.</b> O Spring Security defende-se de
     * enumeração por tempo em {@code DaoAuthenticationProvider.retrieveUser}: quando o usuário não
     * existe, ele chama {@code mitigateAgainstTimingAttack}, que confere a senha apresentada contra
     * um hash fictício e descarta o resultado — só para gastar o mesmo tempo de bcrypt que gastaria
     * se o usuário existisse. Só que esse ramo está em {@code catch (UsernameNotFoundException)}.
     *
     * <p>Um {@code Optional.get()} lançaria {@code NoSuchElementException}, que cai no
     * {@code catch (Exception)} seguinte e <b>nunca</b> chega à mitigação: e-mail inexistente
     * responderia sem pagar bcrypt, visivelmente mais rápido que senha errada. O corpo 401 é
     * idêntico nos dois casos, mas o relógio entregaria quem tem conta aqui.
     *
     * <p>Portanto: {@code UsernameNotFoundException}, sempre. Ela estende
     * {@code AuthenticationException}, então o tratamento existente continua devolvendo o mesmo
     * 401 "Login ou senha inválido" — a mudança é invisível para o cliente, só o tempo muda.
     *
     * <p>Aplicação que registrar o seu próprio {@code UserDetailsService} substitui este bean e
     * assume essa responsabilidade por conta própria.
     */
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public UserDetailsService userDetailsService() {
        return username -> {
            Optional<UserEntity> byEmail = repository.findByEmail(username);
            return byEmail.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        };
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationProvider.class)
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> auditorAware() {
        return new ApplicationAuditAware();
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationManager.class)
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Serviço de cifragem de segredos em repouso (AES-GCM) — usado, entre outros, para o
     * segredo TOTP do MFA. A chave vem de {@code archbase.security.crypto.key} (variável de
     * ambiente/secret manager, nunca versionada).
     */
    @Bean
    @ConditionalOnMissingBean(ArchbaseCryptoService.class)
    public ArchbaseCryptoService archbaseCryptoService(
            @Value("${archbase.security.crypto.key:}") String cryptoKey) {
        return new ArchbaseCryptoService(cryptoKey);
    }

}
