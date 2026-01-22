package br.com.archbase.test.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory para criar contexto de segurança para testes com {@link WithMockArchbaseUser}.
 */
public class WithMockArchbaseUserFactory implements WithSecurityContextFactory<WithMockArchbaseUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockArchbaseUser annotation) {
        SecurityContext context = new SecurityContextImpl();

        // Criar authorities a partir das roles
        List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Criar authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new ArchbaseTestPrincipal(
                        annotation.username(),
                        annotation.tenantId(),
                        annotation.companyId(),
                        annotation.projectId()
                ),
                annotation.password(),
                authorities
        );

        context.setAuthentication(authentication);
        return context;
    }

    /**
     * Principal simples para testes.
     */
    public record ArchbaseTestPrincipal(
            String username,
            String tenantId,
            String companyId,
            String projectId
    ) {
        public ArchbaseTestPrincipal {
            if (username == null || username.isBlank()) {
                username = "test@example.com";
            }
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "test-tenant";
            }
        }

        public String getName() {
            return username;
        }
    }
}
