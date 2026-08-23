package br.com.archbase.security.config;

import br.com.archbase.security.crypto.ArchbaseCryptoService;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Esta configuração precisa poder existir <b>antes</b> do EntityManagerFactory.
 *
 * <p><b>O defeito que este teste fixa.</b> {@code ArchbaseSecurityApplicationConfig} recebia
 * {@code UserJpaRepository} no construtor, o que fazia a classe inteira depender do
 * EntityManagerFactory para ser instanciada. Só que ela também publica o
 * {@code ArchbaseCryptoService} — e o Flyway, que roda <b>antes</b> do EntityManagerFactory, pode
 * precisar dele: numa aplicação cujas migrations Java são beans Spring (padrão do Flyway com
 * {@code JavaMigration}), uma migration que use o re-cifrador fecha o ciclo e a aplicação não sobe:
 *
 * <pre>
 * flyway → migration Java → archbaseColumnReencryptor → ArchbaseCryptoService
 *        → archbaseSecurityApplicationConfig → userJpaRepository
 *        → entityManagerFactory → flyway
 * </pre>
 *
 * <p>Encontrado ao atualizar o mentors-ipaas-api do archbase 3.0.0 para o 3.1.17: a aplicação parou
 * de subir com "The dependencies of some of the beans in the application context form a cycle".
 *
 * <p>O repositório é usado num único ponto — dentro do lambda do {@code UserDetailsService}, ou seja,
 * no login. Resolvê-lo sob demanda mantém o comportamento e desfaz o ciclo.
 */
@DisplayName("A configuração de segurança não exige o repositório para existir")
class ConfiguracaoNaoExigeORepositorioParaExistirTest {

    /**
     * Provedor que <b>explode</b> se alguém pedir o repositório.
     *
     * <p>É o ponto do teste: qualquer resolução do repositório durante a construção da configuração
     * ou do serviço de cifragem significa que o EntityManagerFactory seria exigido ali — que é
     * exatamente o que fechava o ciclo.
     */
    private final ObjectProvider<UserJpaRepository> provedorQueAcusa = new ObjectProvider<>() {
        @Override
        public UserJpaRepository getObject() {
            throw new AssertionError("o repositório foi resolvido cedo demais — isto reintroduz o ciclo com o Flyway");
        }

        @Override
        public UserJpaRepository getObject(Object... args) {
            return getObject();
        }

        @Override
        public UserJpaRepository getIfAvailable() {
            return getObject();
        }

        @Override
        public UserJpaRepository getIfUnique() {
            return getObject();
        }
    };

    @Test
    @DisplayName("a configuração é construída sem tocar no repositório")
    void configuracaoNasceSemRepositorio() {
        assertThatCode(() -> new ArchbaseSecurityApplicationConfig(provedorQueAcusa))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("o serviço de cifragem nasce sem tocar no repositório")
    void cifragemNasceSemRepositorio() {
        // É este bean que o Flyway pode precisar, via re-cifrador, antes do EntityManagerFactory
        // existir. Se ele exigisse o repositório, o ciclo voltaria.
        ArchbaseSecurityApplicationConfig config = new ArchbaseSecurityApplicationConfig(provedorQueAcusa);

        ArchbaseCryptoService cripto = config.archbaseCryptoService(
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

        assertThat(cripto).isNotNull();
    }

    @Test
    @DisplayName("o codificador de senha também")
    void codificadorNasceSemRepositorio() {
        ArchbaseSecurityApplicationConfig config = new ArchbaseSecurityApplicationConfig(provedorQueAcusa);

        assertThat(config.passwordEncoder()).isNotNull();
    }

    @Test
    @DisplayName("mas o login continua usando o repositório — a resolução só saiu de lugar")
    void oLoginSegueUsandoORepositorio() {
        // Controle: se a correção tivesse deixado de consultar o repositório, os três testes acima
        // passariam e a autenticação estaria quebrada. Aqui o provedor que acusa é justamente o que
        // prova que o caminho do login o resolve.
        ArchbaseSecurityApplicationConfig config = new ArchbaseSecurityApplicationConfig(provedorQueAcusa);

        assertThatCode(() -> config.userDetailsService().loadUserByUsername("alguem@exemplo.test"))
                .as("o login precisa resolver o repositório — em runtime, não na subida")
                .isInstanceOf(AssertionError.class);
    }
}
