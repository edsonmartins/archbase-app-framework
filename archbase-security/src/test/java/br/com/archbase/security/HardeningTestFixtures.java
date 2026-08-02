package br.com.archbase.security;

import br.com.archbase.security.config.ArchbaseSecurityHardeningValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Fixture compartilhada dos testes de endurecimento.
 *
 * <p>O {@link #validadorPadrao(long)} reproduz <b>exatamente</b> os defaults declarados nos
 * {@code @Value} de produção. É de propósito: um teste que monta o validador com valores próprios
 * não prova nada sobre o que acontece numa instalação real. Se um default mudar no código sem
 * mudar aqui, os testes de compatibilidade passam a testar outra coisa — por isso a lista está
 * concentrada num lugar só.
 */
public final class HardeningTestFixtures {

    private HardeningTestFixtures() {
    }

    public static ArchbaseSecurityHardeningValidator validadorPadrao(long contagemRetornada) {
        ArchbaseSecurityHardeningValidator validator = new ArchbaseSecurityHardeningValidator();
        ReflectionTestUtils.setField(validator, "entityManager", entityManagerRetornando(contagemRetornada));
        ReflectionTestUtils.setField(validator, "roleResolvers", List.of());
        ReflectionTestUtils.setField(validator, "applicationContext", null);
        ReflectionTestUtils.setField(validator, "validationMode", "fail");
        ReflectionTestUtils.setField(validator, "purgePlaintext", false);
        ReflectionTestUtils.setField(validator, "apiTokenHashEnabled", true);
        ReflectionTestUtils.setField(validator, "noResolverPolicy", "permit");
        ReflectionTestUtils.setField(validator, "adminEndpointsPolicy", "permit");
        ReflectionTestUtils.setField(validator, "strictTokenUse", false);
        ReflectionTestUtils.setField(validator, "logoutEnabled", true);
        ReflectionTestUtils.setField(validator, "logoutUrl", "/api/v1/auth/logout");
        ReflectionTestUtils.setField(validator, "adminGuardEnabled", true);
        ReflectionTestUtils.setField(validator, "allowUnverifiablePrincipal", false);
        return validator;
    }

    public static EntityManager entityManagerRetornando(long contagem) {
        EntityManager em = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(contagem);
        return em;
    }
}
