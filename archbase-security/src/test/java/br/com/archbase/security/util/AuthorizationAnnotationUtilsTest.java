package br.com.archbase.security.util;

import br.com.archbase.security.annotations.RequireProfile;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolução da anotação de autorização.
 *
 * <p>O defeito coberto aqui: as anotações aceitam {@code @Target(TYPE)} e o pointcut intercepta
 * anotação de classe, mas os managers liam apenas {@code method.getAnnotation(...)}. Anotar a
 * <b>classe</b> fazia o interceptador rodar, não achar nada e liberar — pior do que não anotar.
 */
@DisplayName("AuthorizationAnnotationUtils")
class AuthorizationAnnotationUtilsTest {

    @RequireProfile("ADMIN")
    static class AnotadaNaClasse {
        public void metodoSemAnotacao() {
        }
    }

    static class AnotadaNoMetodo {
        @RequireProfile("GERENTE")
        public void metodoAnotado() {
        }
    }

    @RequireProfile("ADMIN")
    static class AnotadaNosDois {
        @RequireProfile("GERENTE")
        public void metodoAnotado() {
        }
    }

    static class SemAnotacao {
        public void metodo() {
        }
    }

    @Test
    @DisplayName("encontra a anotação declarada na classe")
    void encontraNaClasse() throws Exception {
        RequireProfile found = AuthorizationAnnotationUtils.findAnnotation(
                invocationOf(AnotadaNaClasse.class, "metodoSemAnotacao"), RequireProfile.class);

        assertThat(found).isNotNull();
        assertThat(found.value()).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("encontra a anotação declarada no método")
    void encontraNoMetodo() throws Exception {
        RequireProfile found = AuthorizationAnnotationUtils.findAnnotation(
                invocationOf(AnotadaNoMetodo.class, "metodoAnotado"), RequireProfile.class);

        assertThat(found).isNotNull();
        assertThat(found.value()).containsExactly("GERENTE");
    }

    @Test
    @DisplayName("o método vence a classe quando ambos anotam")
    void metodoTemPrecedencia() throws Exception {
        RequireProfile found = AuthorizationAnnotationUtils.findAnnotation(
                invocationOf(AnotadaNosDois.class, "metodoAnotado"), RequireProfile.class);

        assertThat(found).isNotNull();
        assertThat(found.value()).containsExactly("GERENTE");
    }

    @Test
    @DisplayName("devolve null quando não há anotação — o chamador deve negar")
    void semAnotacaoDevolveNull() throws Exception {
        RequireProfile found = AuthorizationAnnotationUtils.findAnnotation(
                invocationOf(SemAnotacao.class, "metodo"), RequireProfile.class);

        assertThat(found).isNull();
    }

    private MethodInvocation invocationOf(Class<?> type, String methodName) throws Exception {
        Object target = type.getDeclaredConstructor().newInstance();
        Method method = type.getMethod(methodName);
        return new MethodInvocation() {
            @Override
            public Method getMethod() {
                return method;
            }

            @Override
            public Object[] getArguments() {
                return new Object[0];
            }

            @Override
            public Object proceed() {
                return null;
            }

            @Override
            public Object getThis() {
                return target;
            }

            @Override
            public AccessibleObject getStaticPart() {
                return method;
            }
        };
    }
}
