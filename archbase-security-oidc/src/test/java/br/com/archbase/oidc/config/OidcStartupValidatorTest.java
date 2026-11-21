package br.com.archbase.oidc.config;

import br.com.archbase.oidc.config.ArchbaseOidcProperties.Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Validação da configuração na subida")
class OidcStartupValidatorTest {

    private static ArchbaseOidcProperties com(String id, Consumer<Provider> ajuste) {
        Provider p = new Provider();
        p.setIssuer("https://idp.example.com");
        p.setAudiences(List.of("app"));
        p.getTenant().setFixed("t1");
        ajuste.accept(p);
        ArchbaseOidcProperties props = new ArchbaseOidcProperties();
        props.getProviders().put(id, p);
        return props;
    }

    private static void recusa(ArchbaseOidcProperties props, String trecho) {
        assertThatThrownBy(() -> OidcStartupValidator.validate(props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(trecho);
    }

    @Test
    @DisplayName("configuração mínima válida sobe")
    void valida() {
        assertThatCode(() -> OidcStartupValidator.validate(com("idp", p -> { }))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sem audiences: recusa (token de outra aplicação passaria)")
    void semAudience() {
        recusa(com("idp", p -> p.setAudiences(List.of())), "audiences");
    }

    @Test
    @DisplayName("algoritmo simétrico ou none: recusa (confusão de algoritmo)")
    void algoritmoSimetrico() {
        recusa(com("idp", p -> p.setAlgorithms(List.of("HS256"))), "HS256");
        recusa(com("idp", p -> p.setAlgorithms(List.of("none"))), "none");
        recusa(com("idp", p -> p.setAlgorithms(List.of())), "algorithms");
    }

    @Test
    @DisplayName("emissor ou JWKS por http sem allow-insecure-http: recusa")
    void http() {
        recusa(com("idp", p -> p.setIssuer("http://idp.example.com")), "https://");
        recusa(com("idp", p -> p.setJwksUri("http://idp.example.com/jwks")), "https://");
        assertThatCode(() -> OidcStartupValidator.validate(com("idp", p -> {
            p.setIssuer("http://localhost:8080");
            p.setAllowInsecureHttp(true);
        }))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("issuer e issuer-pattern juntos, ou nenhum: recusa")
    void emissor() {
        recusa(com("idp", p -> p.setIssuerPattern("https://login.example.com/{tid}/v2.0")), "exatamente um");
        recusa(com("idp", p -> p.setIssuer(null)), "exatamente um");
    }

    @Test
    @DisplayName("issuer-pattern exige {tid}, jwks-uri e allowed-tenants")
    void emissorPorModelo() {
        recusa(com("idp", p -> {
            p.setIssuer(null);
            p.getTenant().setFixed(null);
            p.setIssuerPattern("https://login.example.com/v2.0");
        }), "{tid}");
        recusa(com("idp", p -> {
            p.setIssuer(null);
            p.getTenant().setFixed(null);
            p.setIssuerPattern("https://login.example.com/{tid}/v2.0");
            p.getAllowedTenants().put("a", "t1");
        }), "jwks-uri");
        recusa(com("idp", p -> {
            p.setIssuer(null);
            p.getTenant().setFixed(null);
            p.setIssuerPattern("https://login.example.com/{tid}/v2.0");
            p.setJwksUri("https://login.example.com/keys");
        }), "allowed-tenants");
    }

    @Test
    @DisplayName("nenhuma ou mais de uma fonte de tenant: recusa")
    void tenant() {
        recusa(com("idp", p -> p.getTenant().setFixed(null)), "tenant");
        recusa(com("idp", p -> p.getTenant().setClaim("org")), "tenant");
        recusa(com("idp", p -> p.getTenant().setHeader(true)), "tenant");
    }

    @Test
    @DisplayName("jit com tenant pelo header: recusa (o cliente escolheria o tenant onde criar)")
    void jitComHeader() {
        recusa(com("idp", p -> {
            p.getTenant().setFixed(null);
            p.getTenant().setHeader(true);
            p.getUsers().setProvisioning(ArchbaseOidcProperties.Provisioning.JIT);
        }), "JIT");
    }

    @Test
    @DisplayName("id de provedor fora do formato da coluna: recusa")
    void idInvalido() {
        recusa(com("Meu IdP", p -> { }), "id deve ter");
    }

    @Test
    @DisplayName("dois provedores com o mesmo emissor: recusa")
    void emissorDuplicado() {
        ArchbaseOidcProperties props = com("a", p -> { });
        Provider b = new Provider();
        b.setIssuer("https://idp.example.com");
        b.setAudiences(List.of("app"));
        b.getTenant().setFixed("t1");
        props.getProviders().put("b", b);
        recusa(props, "mesmo emissor");
    }

    @Test
    @DisplayName("todos os erros aparecem de uma vez")
    void todosOsErros() {
        recusa(com("idp", p -> {
            p.setAudiences(List.of());
            p.setAlgorithms(List.of("HS256"));
        }), "audiences");
        recusa(com("idp", p -> {
            p.setAudiences(List.of());
            p.setAlgorithms(List.of("HS256"));
        }), "HS256");
    }
}
