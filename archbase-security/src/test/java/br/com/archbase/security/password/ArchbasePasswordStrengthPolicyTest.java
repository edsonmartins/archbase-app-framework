package br.com.archbase.security.password;

import br.com.archbase.validation.exception.ArchbaseValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ArchbasePasswordStrengthPolicy")
class ArchbasePasswordStrengthPolicyTest {

    private ArchbasePasswordStrengthPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ArchbasePasswordStrengthPolicy();
        desligarTudo();
    }

    private void desligarTudo() {
        ReflectionTestUtils.setField(policy, "minLength", 0);
        ReflectionTestUtils.setField(policy, "requireDigit", false);
        ReflectionTestUtils.setField(policy, "requireUppercase", false);
        ReflectionTestUtils.setField(policy, "requireLowercase", false);
        ReflectionTestUtils.setField(policy, "requireSpecial", false);
        ReflectionTestUtils.setField(policy, "blockCommon", false);
    }

    @Test
    @DisplayName("totalmente desligada, aceita qualquer coisa — atualizar o framework não muda regra de senha")
    void desligadaAceitaTudo() {
        assertThatCode(() -> policy.validate("1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("comprimento mínimo é aplicado")
    void comprimentoMinimo() {
        ReflectionTestUtils.setField(policy, "minLength", 12);

        assertThatThrownBy(() -> policy.validate("curta"))
                .isInstanceOf(ArchbaseValidationException.class)
                .hasMessageContaining("12 caracteres");
        assertThatCode(() -> policy.validate("senhaComTamanhoOk")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("acumula todos os requisitos faltantes numa mensagem só")
    void acumulaProblemas() {
        ReflectionTestUtils.setField(policy, "minLength", 10);
        ReflectionTestUtils.setField(policy, "requireDigit", true);
        ReflectionTestUtils.setField(policy, "requireUppercase", true);
        ReflectionTestUtils.setField(policy, "requireSpecial", true);

        assertThatThrownBy(() -> policy.validate("abc"))
                .isInstanceOf(ArchbaseValidationException.class)
                .hasMessageContaining("10 caracteres")
                .hasMessageContaining("número")
                .hasMessageContaining("maiúscula")
                .hasMessageContaining("especial");
    }

    @Test
    @DisplayName("senha que atende a todas as regras passa")
    void senhaForteePassa() {
        ReflectionTestUtils.setField(policy, "minLength", 10);
        ReflectionTestUtils.setField(policy, "requireDigit", true);
        ReflectionTestUtils.setField(policy, "requireUppercase", true);
        ReflectionTestUtils.setField(policy, "requireLowercase", true);
        ReflectionTestUtils.setField(policy, "requireSpecial", true);

        assertThatCode(() -> policy.validate("Arch#base2026")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("senha comum é recusada mesmo com as demais regras desligadas")
    void senhaComumRecusada() {
        ReflectionTestUtils.setField(policy, "blockCommon", true);

        assertThatThrownBy(() -> policy.validate("Password"))
                .isInstanceOf(ArchbaseValidationException.class)
                .hasMessageContaining("uso comum");
    }

    @Test
    @DisplayName("senha vazia é recusada quando há alguma regra ligada")
    void senhaVaziaRecusada() {
        ReflectionTestUtils.setField(policy, "minLength", 8);

        assertThatThrownBy(() -> policy.validate(null)).isInstanceOf(ArchbaseValidationException.class);
        assertThatThrownBy(() -> policy.validate("")).isInstanceOf(ArchbaseValidationException.class);
    }
}
