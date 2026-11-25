package br.com.archbase.ddd.infraestructure.service;

import br.com.archbase.ddd.domain.contracts.Repository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Até a 3.5.0 os quatro services genéricos chamavam {@code validator.validate(...)} e descartavam o resultado:
 * {@code save} gravava objeto inválido e {@code validate}/{@code validateGroup} nunca recusavam nada.
 */
@DisplayName("Services genéricos recusam objeto inválido")
class ValidacaoDosServicesTest {

    interface Cadastro {
    }

    static class Pessoa {
        @NotBlank
        String nome;
        @Size(max = 3, groups = Cadastro.class)
        String apelido;

        Pessoa(String nome, String apelido) {
            this.nome = nome;
            this.apelido = apelido;
        }
    }

    /** Os quatro services, cada um com um repositório que só registra o que foi gravado. */
    static Stream<Function<Repository<Pessoa, String, Long>, Object>> services() {
        return Stream.of(
                r -> new CommonArchbaseService<Pessoa, String, Long>(r) { },
                r -> new SimpleArchbaseService<Pessoa, String, Long>(r) { },
                r -> new CommonArchbaseCommandService<Pessoa, String, Long>(r) { },
                r -> new SimpleArchbaseCommandService<Pessoa, String, Long>(r) { });
    }

    @ParameterizedTest
    @MethodSource("services")
    @DisplayName("save: objeto inválido lança ConstraintViolationException e não chega ao repositório")
    void saveRecusa(Function<Repository<Pessoa, String, Long>, Object> fabrica) throws Throwable {
        List<Object> gravados = new ArrayList<>();
        Object service = montar(fabrica, gravados);

        assertThatThrownBy(() -> chamar(service, "save", new Pessoa(" ", null)))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(((ConstraintViolationException) e).getConstraintViolations())
                        .extracting(v -> v.getPropertyPath().toString()).containsExactly("nome"));
        assertThat(gravados).isEmpty();

        Pessoa valida = new Pessoa("Ana", "muito longo para o grupo, mas fora do grupo padrão");
        assertThat(chamar(service, "save", valida)).isSameAs(valida);
        assertThat(gravados).containsExactly(valida);
    }

    @ParameterizedTest
    @MethodSource("services")
    @DisplayName("validate e validateGroup recusam; objeto válido passa")
    void validateRecusa(Function<Repository<Pessoa, String, Long>, Object> fabrica) throws Throwable {
        Object service = montar(fabrica, new ArrayList<>());

        assertThatThrownBy(() -> chamar(service, "validate", new Pessoa(null, null)))
                .isInstanceOf(ConstraintViolationException.class);
        chamar(service, "validate", new Pessoa("Ana", null));

        Pessoa apelidoLongo = new Pessoa("Ana", "Aninha");
        assertThatThrownBy(() -> service.getClass().getMethod("validateGroup", Object.class, Class[].class)
                .invoke(service, apelidoLongo, new Class<?>[]{Cadastro.class}))
                .hasCauseInstanceOf(ConstraintViolationException.class);
        service.getClass().getMethod("validateGroup", Object.class, Class[].class)
                .invoke(service, new Pessoa(null, "Ana"), new Class<?>[]{Cadastro.class});
    }

    // ── apoio ────────────────────────────────────────────────────────────────────────────────────

    private static Object chamar(Object service, String metodo, Pessoa pessoa) throws Throwable {
        try {
            return service.getClass().getMethod(metodo, Object.class).invoke(service, pessoa);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    private static Object montar(Function<Repository<Pessoa, String, Long>, Object> fabrica, List<Object> gravados)
            throws Exception {
        Repository<Pessoa, String, Long> repositorio = (Repository<Pessoa, String, Long>) Proxy.newProxyInstance(
                Repository.class.getClassLoader(), new Class<?>[]{Repository.class}, (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        gravados.add(args[0]);
                        return args[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        Object service = fabrica.apply(repositorio);
        Field campo = campo(service.getClass(), "validator");
        campo.setAccessible(true);
        campo.set(service, validator());
        return service;
    }

    private static Field campo(Class<?> tipo, String nome) throws NoSuchFieldException {
        for (Class<?> c = tipo; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(nome);
            } catch (NoSuchFieldException ignorada) {
                // sobe na hierarquia
            }
        }
        throw new NoSuchFieldException(nome);
    }

    private static Validator validator() {
        return Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory().getValidator();
    }
}
