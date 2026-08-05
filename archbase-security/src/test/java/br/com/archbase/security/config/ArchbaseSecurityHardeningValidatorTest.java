package br.com.archbase.security.config;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.spi.ArchbaseRoleResolver;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static br.com.archbase.security.HardeningTestFixtures.dataSourceRetornando;
import static br.com.archbase.security.HardeningTestFixtures.validadorPadrao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O validador existe para transformar "configurei uma proteção que não pode funcionar" em erro de
 * deploy, com instrução, em vez de comportamento errado semanas depois em produção. Estes testes
 * garantem os dois lados: falha quando o pré-requisito não está atendido, e <b>não</b> falha quando
 * está — um validador que impede a subida indevidamente seria pior que a ausência dele.
 */
@DisplayName("ArchbaseSecurityHardeningValidator")
class ArchbaseSecurityHardeningValidatorTest {

    private static class ResolverFake implements ArchbaseRoleResolver {
        @Override
        public Set<String> resolveRoles(UserEntity user) {
            return Set.of();
        }
    }

    @Nested
    @DisplayName("purge-plaintext dos tokens de API")
    class PurgePlaintext {

        @Test
        @DisplayName("falha se ainda há token sem hash — apagar seria irreversível")
        void falhaComTokensSemHash() {
            var validator = validadorPadrao(3L);
            ReflectionTestUtils.setField(validator, "purgePlaintext", true);

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("3 token(s) de API ainda")
                    .hasMessageContaining("suba uma vez SEM purge-plaintext");
        }

        @Test
        @DisplayName("passa quando a migração de hash já rodou")
        void passaComMigracaoConcluida() {
            var validator = validadorPadrao(0L);
            ReflectionTestUtils.setField(validator, "purgePlaintext", true);

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("falha na combinação contraditória com hash-enabled=false")
        void falhaCombinacaoContraditoria() {
            var validator = validadorPadrao(0L);
            ReflectionTestUtils.setField(validator, "purgePlaintext", true);
            ReflectionTestUtils.setField(validator, "apiTokenHashEnabled", false);

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("se contradizem");
        }
    }

    @Nested
    @DisplayName("no-resolver-policy=deny")
    class PoliticaDeRoles {

        @Test
        @DisplayName("falha sem ArchbaseRoleResolver registrado — negaria todo @RequireRole")
        void falhaSemResolver() {
            var validator = validadorPadrao(0L);
            ReflectionTestUtils.setField(validator, "noResolverPolicy", "deny");

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nenhum bean")
                    .hasMessageContaining("ArchbaseRoleResolver");
        }

        @Test
        @DisplayName("passa com resolver registrado")
        void passaComResolver() {
            var validator = validadorPadrao(0L);
            ReflectionTestUtils.setField(validator, "noResolverPolicy", "deny");
            ReflectionTestUtils.setField(validator, "roleResolvers", List.of(new ResolverFake()));

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("strict-token-use")
    class StrictTokenUse {

        @Test
        @DisplayName("falha enquanto há sessões antigas vivas, dizendo quantas seriam derrubadas")
        void falhaComSessoesLegadas() {
            var validator = validadorPadrao(42L);
            ReflectionTestUtils.setField(validator, "strictTokenUse", true);

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("42 sessão(ões) ativas")
                    .hasMessageContaining("refresh-expiration");
        }

        @Test
        @DisplayName("passa quando não sobrou nenhuma sessão legada")
        void passaSemSessoesLegadas() {
            var validator = validadorPadrao(0L);
            ReflectionTestUtils.setField(validator, "strictTokenUse", true);

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("admin-endpoints.policy=permission")
    class PoliticaPorPermissao {

        @Test
        @DisplayName("falha se os Resource/Action não estão cadastrados")
        void falhaSemCadastro() {
            var validator = validadorPadrao(0L);
            ReflectionTestUtils.setField(validator, "adminEndpointsPolicy", "permission");

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MANAGE")
                    .hasMessageContaining("policy=admin-only");
        }

        @Test
        @DisplayName("passa com o cadastro presente")
        void passaComCadastro() {
            var validator = validadorPadrao(1L);
            ReflectionTestUtils.setField(validator, "adminEndpointsPolicy", "permission");

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("admin-only não exige cadastro nenhum")
        void adminOnlyNaoExigeCadastro() {
            var validator = validadorPadrao(0L);
            ReflectionTestUtils.setField(validator, "adminEndpointsPolicy", "admin-only");

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Modo de validação")
    class Modo {

        @Test
        @DisplayName("warn registra o problema mas deixa subir")
        void warnNaoImpedeSubida() {
            var validator = validadorPadrao(5L);
            ReflectionTestUtils.setField(validator, "strictTokenUse", true);
            ReflectionTestUtils.setField(validator, "validationMode", "warn");

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("off nem consulta o banco")
        void offNaoConsulta() {
            DataSource ds = mock(DataSource.class);
            var validator = validadorPadrao(0L);
            ReflectionTestUtils.setField(validator, "dataSource", ds);
            ReflectionTestUtils.setField(validator, "purgePlaintext", true);
            ReflectionTestUtils.setField(validator, "validationMode", "off");

            assertThatCode(validator::validate).doesNotThrowAnyException();
            org.mockito.Mockito.verifyNoInteractions(ds);
        }
    }

    @Test
    @DisplayName("acumula todos os problemas numa mensagem só, numerados")
    void acumulaProblemas() {
        var validator = validadorPadrao(7L);
        ReflectionTestUtils.setField(validator, "purgePlaintext", true);
        ReflectionTestUtils.setField(validator, "strictTokenUse", true);
        ReflectionTestUtils.setField(validator, "noResolverPolicy", "deny");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1)")
                .hasMessageContaining("2)")
                .hasMessageContaining("3)")
                .hasMessageContaining("deployment/security-hardening.md");
    }

    @Test
    @DisplayName("falha de consulta ao banco não vira falha de validação")
    void erroDeConsultaNaoBloqueia() {
        DataSource ds = mock(DataSource.class);
        try {
            when(ds.getConnection()).thenThrow(new IllegalStateException("tabela ausente"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        var validator = validadorPadrao(0L);
        ReflectionTestUtils.setField(validator, "dataSource", ds);
        ReflectionTestUtils.setField(validator, "purgePlaintext", true);

        // Schema desatualizado é outro problema, com outra mensagem. Mascará-lo aqui esconderia a
        // causa real atrás de um erro de segurança que não é o que está acontecendo.
        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a mensagem diz o que fazer, não só o que está errado")
    void mensagemEhAcionavel() {
        var validator = validadorPadrao(1L);
        ReflectionTestUtils.setField(validator, "purgePlaintext", true);

        String mensagem = assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .actual().getMessage();

        assertThat(mensagem)
                .contains("O que fazer:")
                .contains("archbase.security.hardening.validation=warn");
    }

    @Test
    @DisplayName("DataSource alternativo é usado (sanidade do mock compartilhado)")
    void sanidadeDoMock() {
        var validator = validadorPadrao(0L);
        ReflectionTestUtils.setField(validator, "dataSource", dataSourceRetornando(9L));
        ReflectionTestUtils.setField(validator, "strictTokenUse", true);

        assertThatThrownBy(validator::validate).hasMessageContaining("9 sessão(ões)");
    }
}
