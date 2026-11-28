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
     * Principal simples para testes. Implementa {@link java.security.Principal}, então
     * {@code authentication.getName()} devolve o {@code username}; até a 3.5.0 devolvia o {@code toString()} do
     * record.
     *
     * <p>Não é a {@code UserEntity} do {@code archbase-security}: {@code @HasPermission} nega e
     * {@code ArchbaseCurrentUser} não o reconhece. {@code tenantId}, {@code companyId} e {@code projectId} ficam só no
     * principal; não preenchem o {@code ArchbaseTenantContext}.
     */
    public record ArchbaseTestPrincipal(
            String username,
            String tenantId,
            String companyId,
            String projectId
    ) implements java.security.Principal {
        public ArchbaseTestPrincipal {
            if (username == null || username.isBlank()) {
                username = "test@example.com";
            }
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "test-tenant";
            }
        }

        @Override
        public String getName() {
            return username;
        }
    }
}
