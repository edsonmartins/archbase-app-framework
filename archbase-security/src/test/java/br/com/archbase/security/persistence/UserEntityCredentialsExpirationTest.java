package br.com.archbase.security.persistence;

import br.com.archbase.security.password.ArchbasePasswordPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre a semântica de {@code isCredentialsNonExpired()} — antes da correção, o método
 * retornava {@code passwordNeverExpires} direto, o que travava o login de qualquer usuário
 * com {@code BO_SENHA_NUNCA_EXPIRA = 'N'}, inclusive logo após redefinir a senha.
 */
class UserEntityCredentialsExpirationTest {

    @AfterEach
    void resetPolicy() {
        ArchbasePasswordPolicy.setExpirationDays(ArchbasePasswordPolicy.NO_EXPIRATION);
    }

    private UserEntity user(Boolean changeOnNextLogin, Boolean neverExpires, LocalDateTime changedAt) {
        return UserEntity.builder()
                .userName("fulano")
                .password("hash")
                .changePasswordOnNextLogin(changeOnNextLogin)
                .passwordNeverExpires(neverExpires)
                .passwordChangedAt(changedAt)
                .build();
    }

    @Test
    @DisplayName("passwordNeverExpires=false sozinho não expira credenciais (política desligada)")
    void naoExpiraApenasPorNeverExpiresFalso() {
        assertTrue(user(false, false, LocalDateTime.now().minusYears(5)).isCredentialsNonExpired());
    }

    @Test
    @DisplayName("changePasswordOnNextLogin=true expira credenciais")
    void expiraQuandoTrocaObrigatoria() {
        assertFalse(user(true, true, LocalDateTime.now()).isCredentialsNonExpired());
    }

    @Test
    @DisplayName("markPasswordChanged libera o login após o reset de senha")
    void resetDeSenhaLiberaLogin() {
        UserEntity user = user(true, false, null);
        assertFalse(user.isCredentialsNonExpired());

        user.markPasswordChanged();

        assertTrue(user.isCredentialsNonExpired());
        assertFalse(user.getChangePasswordOnNextLogin());
    }

    @Test
    @DisplayName("com política ligada, senha além do prazo expira; dentro do prazo não")
    void respeitaPoliticaDeValidade() {
        ArchbasePasswordPolicy.setExpirationDays(90);

        assertFalse(user(false, false, LocalDateTime.now().minusDays(91)).isCredentialsNonExpired());
        assertTrue(user(false, false, LocalDateTime.now().minusDays(89)).isCredentialsNonExpired());
    }

    @Test
    @DisplayName("passwordNeverExpires=true ignora a política de validade")
    void neverExpiresIgnoraPolitica() {
        ArchbasePasswordPolicy.setExpirationDays(90);

        assertTrue(user(false, true, LocalDateTime.now().minusYears(5)).isCredentialsNonExpired());
    }

    @Test
    @DisplayName("usuário legado sem data de troca não expira ao ligar a política")
    void legadoSemDataNaoExpira() {
        ArchbasePasswordPolicy.setExpirationDays(90);

        assertTrue(user(false, false, null).isCredentialsNonExpired());
    }

    @Test
    @DisplayName("flags nulas não estouram NullPointerException")
    void flagsNulasNaoQuebram() {
        UserEntity user = user(null, null, null);

        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isEnabled());
    }
}
