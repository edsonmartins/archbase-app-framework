package br.com.archbase.security.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Em qual banco a rotina de esquema vai escrever.
 *
 * <p><b>O defeito que estes testes fixam.</b> A primeira versão exigia um único {@code DataSource} e
 * desistia quando havia mais de um. Parecia prudente, e era o contrário: aplicações com dois bancos
 * são comuns — um relacional principal e um legado somente de leitura — e é justamente nelas que
 * ninguém quer descobrir na mão qual coluna o framework passou a exigir. A rotina ficava inerte
 * exatamente onde era mais necessária, e sem alarde.
 *
 * <p>Isso não apareceu em teste algum porque toda a suíte roda com um único DataSource. Apareceu ao
 * conferir a configuração de uma aplicação real antes de atualizá-la.
 */
class EscolhaDoBancoDaSegurancaTest {

    private final ArchbaseSecuritySchemaProperties propriedades = new ArchbaseSecuritySchemaProperties();

    @Test
    @DisplayName("com dois bancos e um @Primary, escreve no @Primary")
    void doisBancosComPrimario() {
        DataSource principal = mock(DataSource.class);
        DataSource legado = mock(DataSource.class);

        // getIfUnique() é o método que respeita @Primary: com um primário declarado, ele devolve esse
        // — é assim que o Spring resolve a mesma ambiguidade em qualquer injeção.
        Provedor<DataSource> provedor = new Provedor<>(List.of(principal, legado), principal);

        assertThat(resolver(provedor)).isSameAs(principal);
    }

    @Test
    @DisplayName("com dois bancos e nenhum @Primary, não escreve em lugar nenhum")
    void doisBancosSemPrimario() {
        // Aqui desistir é a resposta certa, e é a diferença que importa: escrever no banco errado
        // criaria as tabelas onde ninguém vai procurá-las, enquanto o banco de verdade segue sem
        // elas — e a aplicação continuaria quebrando com a mensagem que a rotina deveria ter
        // eliminado.
        Provedor<DataSource> provedor = new Provedor<>(List.of(mock(DataSource.class), mock(DataSource.class)), null);

        assertThat(resolver(provedor)).isNull();
    }

    @Test
    @DisplayName("com um único banco, escreve nele")
    void bancoUnico() {
        DataSource unico = mock(DataSource.class);
        Provedor<DataSource> provedor = new Provedor<>(List.of(unico), unico);

        assertThat(resolver(provedor)).isSameAs(unico);
    }

    @Test
    @DisplayName("sem banco nenhum, não faz nada")
    void semBanco() {
        assertThat(resolver(new Provedor<>(List.of(), null))).isNull();
    }

    /**
     * Exercita a escolha sem EntityManagerFactory, que é o caminho do {@code @Primary}.
     *
     * <p>O caminho preferido — perguntar ao EntityManagerFactory qual DataSource ele usa — é
     * exercitado pelos testes de integração, onde há um Hibernate de verdade para responder.
     */
    private DataSource resolver(ObjectProvider<DataSource> dataSources) {
        ArchbaseSecuritySchemaInitializer initializer = new ArchbaseSecuritySchemaInitializer(
                dataSources, new Provedor<>(List.of(), null), propriedades);
        return initializer.bancoEscolhido();
    }

    /** Dublê de {@link ObjectProvider} que distingue "quantos existem" de "qual é o principal". */
    private static class Provedor<T> implements ObjectProvider<T> {

        private final List<T> todos;
        private final T primario;

        Provedor(List<T> todos, T primario) {
            this.todos = todos;
            this.primario = primario;
        }

        @Override
        public T getObject() {
            return todos.get(0);
        }

        @Override
        public T getObject(Object... args) {
            return getObject();
        }

        @Override
        public T getIfAvailable() {
            return todos.isEmpty() ? null : todos.get(0);
        }

        @Override
        public T getIfUnique() {
            return primario;
        }

        @Override
        public Stream<T> stream() {
            return todos.stream();
        }

        @Override
        public Stream<T> orderedStream() {
            return todos.stream();
        }
    }
}
