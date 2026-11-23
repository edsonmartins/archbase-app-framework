package br.com.archbase.oidc.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Os exemplos de provedor do README sobem. O arquivo {@code docs/exemplos-provedores.yml} é a cópia exata do que
 * está no README: se um exemplo deixar de passar no bind ou no {@link OidcStartupValidator}, o build quebra antes de
 * alguém copiar uma configuração que não sobe.
 */
class ExemplosDocumentadosTest {

    @Test
    void exemplosDoReadmePassamNoBindENaValidacao() throws Exception {
        List<PropertySource<?>> yaml = new YamlPropertySourceLoader()
                .load("exemplos", new ClassPathResource("docs/exemplos-provedores.yml"));
        MutablePropertySources sources = new MutablePropertySources();
        yaml.forEach(sources::addLast);
        Binder binder = new Binder(ConfigurationPropertySources.from(sources),
                new PropertySourcesPlaceholdersResolver(sources));

        ArchbaseOidcProperties properties = binder.bind("archbase.security.oidc", ArchbaseOidcProperties.class)
                .orElseThrow(() -> new AssertionError("nada sob archbase.security.oidc"));

        assertThat(properties.getProviders()).containsOnlyKeys("archguard", "keycloak", "entra", "auth0", "okta");
        assertThatCode(() -> OidcStartupValidator.validate(properties)).doesNotThrowAnyException();

        // O que um erro de indentação ou de chave faria sumir em silêncio.
        assertThat(properties.getProviders().get("archguard").getTenant().getClaim()).isEqualTo("owner");
        assertThat(properties.getProviders().get("archguard").getIntrospection().getClientSecret()).isEqualTo("troque");
        assertThat(properties.getProviders().get("keycloak").getTenant().getFixed()).isEqualTo("empresa");
        assertThat(properties.getProviders().get("keycloak").getRoleMapping().getMap())
                .containsEntry("empresa/vendas", "Vendas");
        assertThat(properties.getProviders().get("entra").getAllowedTenants())
                .containsEntry("11111111-2222-3333-4444-555555555555", "cliente-a");
        assertThat(properties.getProviders().get("entra").getClaims().getSubject()).isEqualTo("oid");
        assertThat(properties.getProviders().get("auth0").getClaims().getEmail()).isEqualTo("https://empresa.com/email");
        assertThat(properties.getProviders().get("okta").getTenant().isHeader()).isTrue();
        assertThat(properties.getProviders().get("okta").getUsers().getLinkByEmail())
                .isEqualTo(ArchbaseOidcProperties.LinkByEmail.NEVER);
    }

    /** O bloco YAML do README é este arquivo, sem tirar nem pôr — senão o teste acima valida outra coisa. */
    @Test
    void readmeTrazExatamenteOsExemplosTestados() throws Exception {
        String readme = Files.readString(Path.of("README.md"), StandardCharsets.UTF_8);
        String bloco = readme.split("```yaml\n", 2)[1].split("```", 2)[0];
        String arquivo = new ClassPathResource("docs/exemplos-provedores.yml")
                .getContentAsString(StandardCharsets.UTF_8);
        arquivo = arquivo.substring(arquivo.indexOf("archbase:"));
        assertThat(bloco.strip()).isEqualTo(arquivo.strip());
    }
}
