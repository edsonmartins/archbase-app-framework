package br.com.archbase.security;

import br.com.archbase.security.config.ArchbaseSecurityHardeningValidator;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

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
        ReflectionTestUtils.setField(validator, "dataSource", dataSourceRetornando(contagemRetornada));
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

    /**
     * DataSource que devolve {@code contagem} em qualquer consulta.
     *
     * <p>Substituiu o dublê de {@code EntityManager}: o validador passou a contar por conexão JDBC
     * própria, porque uma consulta que falha pelo EntityManager marca a transação como
     * rollback-only e derruba a subida a partir do listener de ApplicationReadyEvent.
     */
    public static DataSource dataSourceRetornando(long contagem) {
        try {
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getLong(1)).thenReturn(contagem);

            Statement statement = mock(Statement.class);
            when(statement.executeQuery(anyString())).thenReturn(rs);

            PreparedStatement ps = mock(PreparedStatement.class);
            when(ps.executeQuery()).thenReturn(rs);

            Connection conexao = mock(Connection.class);
            when(conexao.createStatement()).thenReturn(statement);
            when(conexao.prepareStatement(anyString())).thenReturn(ps);

            DataSource dataSource = mock(DataSource.class);
            when(dataSource.getConnection()).thenReturn(conexao);
            return dataSource;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
