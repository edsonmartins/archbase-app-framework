package teste.starter;

import br.com.archbase.ddd.domain.contracts.Repository;
import br.com.archbase.ddd.infraestructure.service.CommonArchbaseService;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Um service que estende {@link CommonArchbaseService}, com o {@code archbase-starter}: sobe (até a 3.5.0 a injeção
 * exigia um bean chamado {@code validator}, que no Boot 4 não é criado), recusa o objeto inválido sem gravar, e a
 * API responde 400 com o campo — não 500.
 */
@DisplayName("Service genérico com o archbase-starter: validação de ponta a ponta")
class ViolacaoDeRestricaoNaApiTest {

    public static class Pessoa {
        @NotBlank
        public String nome;

        public Pessoa(String nome) {
            this.nome = nome;
        }
    }

    public static class PessoaService extends CommonArchbaseService<Pessoa, String, Long> {
        public PessoaService(Repository<Pessoa, String, Long> repository) {
            super(repository);
        }
    }

    @RestController
    public static class PessoaController {
        private final PessoaService service;

        public PessoaController(PessoaService service) {
            this.service = service;
        }

        @PostMapping("/teste/pessoa")
        public void criar(@RequestParam String nome) {
            service.save(new Pessoa(nome));
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
        static final List<Object> GRAVADOS = new CopyOnWriteArrayList<>();

        @Bean
        @SuppressWarnings("unchecked")
        PessoaService pessoaService() {
            Repository<Pessoa, String, Long> repositorio = (Repository<Pessoa, String, Long>) Proxy.newProxyInstance(
                    Repository.class.getClassLoader(), new Class<?>[]{Repository.class}, (proxy, method, args) -> {
                        if (method.getName().equals("save")) {
                            GRAVADOS.add(args[0]);
                            return args[0];
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            return new PessoaService(repositorio);
        }

        @Bean
        PessoaController pessoaController(PessoaService service) {
            return new PessoaController(service);
        }
    }

    @Test
    @DisplayName("inválido: 400 com o campo e nada gravado; válido: 200 e gravado")
    void pontaAPonta() throws Exception {
        App.GRAVADOS.clear();
        try (ConfigurableApplicationContext ctx = SubidaDoStarterCompletoTest.subir(App.class,
                "--archbase.security.whitelist=/teste/**")) {
            var mvc = SubidaDoStarterCompletoTest.mockMvc(ctx);

            mvc.perform(post("/teste/pessoa").param("nome", " "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].property").value("nome"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED_NOT_BLANK"));
            assertThat(App.GRAVADOS).isEmpty();

            mvc.perform(post("/teste/pessoa").param("nome", "Ana")).andExpect(status().isOk());
            assertThat(App.GRAVADOS).hasSize(1);
        }
    }
}
