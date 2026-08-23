package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.starter.core.auto.configuration.entidades.EntidadeDeTeste;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O registrar informa pacotes de entidades à fábrica de EntityManager — e só isso.
 *
 * <p>Antes ele usava um scanner que <b>registra beans</b>, o que transformava cada entidade da
 * aplicação em singleton do container. Isso derrubava a subida no Spring 7: o
 * {@code PersistenceAnnotationBeanPostProcessor} usa o bean como chave de mapa, chamando
 * {@code hashCode()} numa entidade recém-criada de campos nulos.
 */
class ArchbaseAdditionalScanConfigurerTest {

    private static final String PACOTE = "br.com.archbase.starter.core.auto.configuration.entidades";

    @Test
    @DisplayName("os pacotes de entidade chegam ao EntityScanPackages")
    void pacotesChegamAoEntityScan() {
        try (AnnotationConfigApplicationContext contexto = contextoCom("archbase.app.jpa.entities", PACOTE)) {
            assertThat(EntityScanPackages.get(contexto).getPackageNames()).contains(PACOTE);
        }
    }

    @Test
    @DisplayName("entidade não vira bean do container")
    void entidadeNaoViraBean() {
        try (AnnotationConfigApplicationContext contexto = contextoCom("archbase.app.jpa.entities", PACOTE)) {
            // Este era o defeito: a entidade aparecia como singleton e era instanciada na subida.
            assertThat(contexto.getBeanNamesForType(EntidadeDeTeste.class)).isEmpty();
        }
    }

    @Test
    @DisplayName("sem a propriedade, nada é registrado")
    void semPropriedadeNadaAcontece() {
        try (AnnotationConfigApplicationContext contexto = contextoCom(null, null)) {
            assertThat(contexto.getBeanNamesForType(EntidadeDeTeste.class)).isEmpty();
            assertThat(contexto.getBeanNamesForType(EntityManagerFactory.class)).isEmpty();
        }
    }

    private AnnotationConfigApplicationContext contextoCom(String propriedade, String valor) {
        MockEnvironment ambiente = new MockEnvironment();
        if (propriedade != null) {
            ambiente.setProperty(propriedade, valor);
        }
        AnnotationConfigApplicationContext contexto =
                new AnnotationConfigApplicationContext(new DefaultListableBeanFactory());
        contexto.setEnvironment(ambiente);
        contexto.register(ConfiguracaoDeTeste.class);
        contexto.refresh();
        return contexto;
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ArchbaseAdditionalScanConfigurer.class)
    static class ConfiguracaoDeTeste {
    }
}
