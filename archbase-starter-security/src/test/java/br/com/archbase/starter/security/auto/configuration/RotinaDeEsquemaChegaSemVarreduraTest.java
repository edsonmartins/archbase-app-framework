package br.com.archbase.starter.security.auto.configuration;

import br.com.archbase.security.schema.ArchbaseSecuritySchemaInitializer;
import br.com.archbase.security.schema.ArchbaseSecuritySchemaProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * A rotina de esquema precisa chegar a quem <b>não</b> varre {@code br.com.archbase}.
 *
 * <p><b>O defeito que este teste fixa.</b> A rotina foi entregue na 3.1.12 como configuração comum
 * dentro do módulo de segurança, e configuração comum só existe se alguém a varrer. Aplicações que
 * varrem apenas os próprios pacotes — o caso normal — nunca a registravam. Não havia erro nem log:
 * ela simplesmente não existia, e "o framework cuida do próprio esquema" valia só para parte dos
 * consumidores.
 *
 * <p>Nenhum teste pegou isso porque a aplicação de teste do módulo de segurança varre
 * {@code br.com.archbase.security} inteiro — exatamente o arranjo em que o defeito não aparece. Este
 * roda <b>sem varredura nenhuma</b>: só as auto-configurações, que é o que um consumidor recebe ao
 * declarar a dependência.
 */
class RotinaDeEsquemaChegaSemVarreduraTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ArchbaseSecuritySchemaAutoConfiguration.class))
            // DataSource de mentira: o que se verifica aqui é o REGISTRO do bean, não o que ele faz
            // com o banco — daí o modo desligado. O comportamento contra banco de verdade é
            // exercitado no archbase-security, em H2, PostgreSQL e MySQL.
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withPropertyValues("archbase.security.schema.mode=off");

    @Test
    @DisplayName("o inicializador é registrado só por estar no classpath")
    void inicializadorEhRegistrado() {
        runner.run(contexto -> assertThat(contexto).hasSingleBean(ArchbaseSecuritySchemaInitializer.class));
    }

    @Test
    @DisplayName("as propriedades de esquema são ligadas")
    void propriedadesSaoLigadas() {
        // Em modo desligado a rotina não vai ao banco, então o DataSource de mentira não atrapalha e
        // sobra só o que interessa aqui: as propriedades chegaram ao bean.
        runner.withPropertyValues("archbase.security.schema.fail-on-error=true")
                .run(contexto -> {
                    ArchbaseSecuritySchemaProperties propriedades =
                            contexto.getBean(ArchbaseSecuritySchemaProperties.class);
                    assertThat(propriedades.getMode()).isEqualTo(ArchbaseSecuritySchemaProperties.Mode.OFF);
                    assertThat(propriedades.isFailOnError()).isTrue();
                });
    }

    @Test
    @DisplayName("por padrão, falhar ao conferir o esquema não derruba a aplicação")
    void falhaNaoDerrubaPorPadrao() {
        // O DataSource de mentira não responde a nada, então a conferência FALHA de verdade — é o
        // cenário de quem não tem permissão de DDL. A aplicação precisa subir assim mesmo: uma
        // rotina acessória que impede o serviço de existir é pior do que a ausência dela.
        runner.withPropertyValues("archbase.security.schema.mode=apply")
                .run(contexto -> assertThat(contexto).hasNotFailed());
    }

    @Test
    @DisplayName("mas com fail-on-error=true ela derruba, como prometido")
    void falhaDerrubaQuandoPedido() {
        // O par do teste acima. Sem ele, "fail-on-error" poderia estar sendo ignorado e a suíte
        // seguiria verde — quem liga a chave em homologação justamente para descobrir o problema
        // antes da produção não descobriria nada.
        runner.withPropertyValues("archbase.security.schema.mode=apply",
                        "archbase.security.schema.fail-on-error=true")
                .run(contexto -> assertThat(contexto).hasFailed());
    }

    @Test
    @DisplayName("archbase.security.enabled=false não registra nada")
    void desligarSegurancaNaoRegistra() {
        // Quem desliga a segurança do framework não espera que ele mexa no esquema de segurança.
        runner.withPropertyValues("archbase.security.enabled=false")
                .run(contexto -> assertThat(contexto).doesNotHaveBean(ArchbaseSecuritySchemaInitializer.class));
    }

    @Test
    @DisplayName("quem também varre br.com.archbase não ganha um segundo inicializador")
    void varreduraMaisAutoConfiguracaoNaoDuplica() {
        // O caso de quem JÁ está em produção: aplicações que varrem br.com.archbase encontram a
        // configuração pelos dois caminhos a partir desta versão. Se isso registrasse dois beans, a
        // correção quebraria justamente quem já estava funcionando — e o DDL rodaria duas vezes.
        runner.withUserConfiguration(SimulaVarreduraDaAplicacao.class)
                .run(contexto -> {
                    assertThat(contexto).hasNotFailed();
                    assertThat(contexto).hasSingleBean(ArchbaseSecuritySchemaInitializer.class);
                });
    }

    /** Reproduz o {@code @ComponentScan} que alcança o pacote da rotina. */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.context.annotation.ComponentScan(
            basePackageClasses = ArchbaseSecuritySchemaInitializer.class)
    static class SimulaVarreduraDaAplicacao {
    }

    @Test
    @DisplayName("um inicializador declarado pela aplicação tem precedência")
    void inicializadorProprioVence() {
        // É a saída documentada para quem tem mais de um banco e precisa apontar o certo. Se a
        // auto-configuração registrasse o seu por cima, essa saída deixaria de existir.
        runner.withBean("meuInicializador", ArchbaseSecuritySchemaInitializer.class,
                        () -> new ArchbaseSecuritySchemaInitializer(null, null, new ArchbaseSecuritySchemaProperties()))
                .run(contexto -> {
                    assertThat(contexto).hasSingleBean(ArchbaseSecuritySchemaInitializer.class);
                    assertThat(contexto).hasBean("meuInicializador");
                });
    }
}
