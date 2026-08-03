package br.com.archbase.security.docs;

import br.com.archbase.security.access.AccessLevel;
import br.com.archbase.security.annotation.ArchbaseResource;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.annotations.RequireProfile;
import br.com.archbase.security.annotations.RequireRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os exemplos da documentação, compilando de verdade.
 *
 * <p><b>Por que este teste existe.</b> A auditoria encontrou, na documentação que os desenvolvedores
 * usavam, exemplos de {@code @HasPermission} <b>sem {@code description}</b> — que não compilam,
 * porque o atributo não tem valor padrão. Alguém copiava, colava, e o erro aparecia no build sem
 * relação óbvia com o que se pretendia fazer.
 *
 * <p>Documentação que não compila é pior do que documentação ausente: ela custa tempo antes de
 * revelar que estava errada. Copiar os exemplos para dentro do código de teste faz o build ser o
 * revisor — se um exemplo deixar de valer, o build quebra junto.
 *
 * <p>Os blocos abaixo são cópias dos exemplos de {@code ARQUITETURA.md} e {@code readme-security.md}.
 * Ao editar um deles, edite o outro.
 */
@DisplayName("Exemplos da documentação")
class ExemplosDaDocumentacaoTest {

    // ---------------------------------------------------------- ARQUITETURA.md → @HasPermission

    @ArchbaseResource(value = "tms.ordemservico", description = "Ordem de serviço")
    static class OrdemServicoController {

        @HasPermission(action = "view", description = "Listar ordens de serviço")
        public void listar() {
        }

        @HasPermission(action = "iniciar_execucao", description = "Iniciar a execução da OS",
                minimumLevel = AccessLevel.OPERATOR)
        public void iniciar() {
        }

        @HasPermission(action = "aprovar_custo", description = "Aprovar o custo da OS",
                minimumLevel = AccessLevel.SUPERVISOR)
        public void aprovarCusto() {
        }

        @RequireProfile(value = "SUPERVISOR", allowSystemAdmin = false)
        @HasPermission(action = "cancelar", description = "Cancelar a OS",
                minimumLevel = AccessLevel.SUPERVISOR)
        public void cancelar() {
        }
    }

    // ---------------------------------------------------------- ARQUITETURA.md → receita do CRUD

    @ArchbaseResource(value = "tms.veiculo", description = "Veículo")
    static class VeiculoController {

        @HasPermission(action = "view", description = "Listar veículos")
        public void listar() {
        }

        @HasPermission(action = "create", description = "Cadastrar veículo",
                minimumLevel = AccessLevel.OPERATOR)
        public void criar() {
        }

        @HasPermission(action = "edit", description = "Editar veículo",
                minimumLevel = AccessLevel.OPERATOR)
        public void editar() {
        }

        @HasPermission(action = "delete", description = "Excluir veículo",
                minimumLevel = AccessLevel.SUPERVISOR)
        public void excluir() {
        }
    }

    // ---------------------------------------------------------- ARQUITETURA.md → as trancas

    static class TrancasController {

        @RequireProfile("SUPERVISOR")
        public void fecharCompetencia() {
        }

        @RequireProfile(value = "AUDITORIA", allowSystemAdmin = false)
        public void exportarTrilha() {
        }

        @RequireRole("GESTOR_FROTA")
        public void reatribuirVeiculo() {
        }
    }

    // ---------------------------------------------------------- verificações

    @Test
    @DisplayName("o recurso da classe é herdado quando o método não declara")
    void recursoHerdado() throws Exception {
        Method listar = OrdemServicoController.class.getMethod("listar");

        assertThat(listar.getAnnotation(HasPermission.class).resource())
                .as("o método não declara resource — quem responde é @ArchbaseResource")
                .isEmpty();
        assertThat(OrdemServicoController.class.getAnnotation(ArchbaseResource.class).value())
                .isEqualTo("tms.ordemservico");
    }

    @Test
    @DisplayName("description é obrigatória — nenhum exemplo pode omiti-la")
    void descriptionSemprePresente() {
        for (Class<?> controller : java.util.List.of(OrdemServicoController.class, VeiculoController.class)) {
            for (Method metodo : controller.getDeclaredMethods()) {
                HasPermission anotacao = metodo.getAnnotation(HasPermission.class);
                if (anotacao == null) {
                    continue;
                }
                assertThat(anotacao.description())
                        .as("%s#%s sem description", controller.getSimpleName(), metodo.getName())
                        .isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("minimumLevel omitido é NONE — ausência de piso")
    void minimumLevelOmitidoENone() throws Exception {
        assertThat(OrdemServicoController.class.getMethod("listar")
                .getAnnotation(HasPermission.class).minimumLevel())
                .isEqualTo(AccessLevel.NONE);

        assertThat(OrdemServicoController.class.getMethod("aprovarCusto")
                .getAnnotation(HasPermission.class).minimumLevel())
                .isEqualTo(AccessLevel.SUPERVISOR);
    }

    @Test
    @DisplayName("anotações combinadas coexistem no mesmo método")
    void combinadasCoexistem() throws Exception {
        Method cancelar = OrdemServicoController.class.getMethod("cancelar");

        assertThat(cancelar.getAnnotation(HasPermission.class)).isNotNull();
        assertThat(cancelar.getAnnotation(RequireProfile.class)).isNotNull();
        assertThat(cancelar.getAnnotation(RequireProfile.class).allowSystemAdmin())
                .as("o exemplo demonstra a tranca valendo também para o administrador")
                .isFalse();
    }

    @Test
    @DisplayName("os defaults das trancas são os que a documentação afirma")
    void defaultsDasTrancas() throws Exception {
        RequireProfile semDeclarar = TrancasController.class
                .getMethod("fecharCompetencia").getAnnotation(RequireProfile.class);

        assertThat(semDeclarar.allowSystemAdmin())
                .as("documentado como 'nasce true'")
                .isTrue();
        assertThat(semDeclarar.requireActiveUser()).isTrue();
        assertThat(semDeclarar.requireAll()).isFalse();

        RequireRole role = TrancasController.class
                .getMethod("reatribuirVeiculo").getAnnotation(RequireRole.class);
        assertThat(role.allowSystemAdmin()).isTrue();
        assertThat(role.requirePlatformAdmin()).isFalse();
    }
}
