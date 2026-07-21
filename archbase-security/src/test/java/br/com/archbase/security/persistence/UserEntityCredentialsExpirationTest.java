package br.com.archbase.security.persistence;

import br.com.archbase.security.password.ArchbasePasswordPolicy;
import org.junit.After;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Cobre a semântica de {@code isCredentialsNonExpired()} — antes da correção, o método
 * retornava {@code passwordNeverExpires} direto, o que travava o login de qualquer usuário
 * com {@code BO_SENHA_NUNCA_EXPIRA = 'N'}, inclusive logo após redefinir a senha.
 *
 * <p>Escrito em JUnit 4 porque nesta linha (2.1.x) o surefire resolve o provider JUnit 4:
 * o pom raiz fixa junit-jupiter-engine 5.2.0 e junit-platform-runner 1.2.0 para sustentar
 * os testes legados dos demais módulos.
 */
public class UserEntityCredentialsExpirationTest {

    @After
    public void resetPolicy() {
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

    /** passwordNeverExpires=false sozinho não expira credenciais (política desligada). */
    @Test
    public void naoExpiraApenasPorNeverExpiresFalso() {
        assertTrue(user(false, false, LocalDateTime.now().minusYears(5)).isCredentialsNonExpired());
    }

    /** changePasswordOnNextLogin=true expira credenciais. */
    @Test
    public void expiraQuandoTrocaObrigatoria() {
        assertFalse(user(true, true, LocalDateTime.now()).isCredentialsNonExpired());
    }

    /** markPasswordChanged libera o login após o reset de senha. */
    @Test
    public void resetDeSenhaLiberaLogin() {
        UserEntity user = user(true, false, null);
        assertFalse(user.isCredentialsNonExpired());

        user.markPasswordChanged();

        assertTrue(user.isCredentialsNonExpired());
        assertFalse(user.getChangePasswordOnNextLogin());
    }

    /** Com política ligada, senha além do prazo expira; dentro do prazo não. */
    @Test
    public void respeitaPoliticaDeValidade() {
        ArchbasePasswordPolicy.setExpirationDays(90);

        assertFalse(user(false, false, LocalDateTime.now().minusDays(91)).isCredentialsNonExpired());
        assertTrue(user(false, false, LocalDateTime.now().minusDays(89)).isCredentialsNonExpired());
    }

    /** passwordNeverExpires=true ignora a política de validade. */
    @Test
    public void neverExpiresIgnoraPolitica() {
        ArchbasePasswordPolicy.setExpirationDays(90);

        assertTrue(user(false, true, LocalDateTime.now().minusYears(5)).isCredentialsNonExpired());
    }

    /** Usuário legado sem data de troca não expira ao ligar a política. */
    @Test
    public void legadoSemDataNaoExpira() {
        ArchbasePasswordPolicy.setExpirationDays(90);

        assertTrue(user(false, false, null).isCredentialsNonExpired());
    }

    /** Flags nulas não estouram NullPointerException. */
    @Test
    public void flagsNulasNaoQuebram() {
        UserEntity user = user(null, null, null);

        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isEnabled());
    }
}
