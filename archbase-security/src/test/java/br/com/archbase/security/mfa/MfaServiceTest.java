package br.com.archbase.security.mfa;

import br.com.archbase.security.crypto.ArchbaseCryptoService;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MfaService — configuração/ativação/verificação do 2FA: segredo cifrado em repouso,
 * validação TOTP e códigos de recuperação de uso único.
 */
@DisplayName("MfaService — MFA/TOTP")
class MfaServiceTest {

    private final UserJpaRepository repository = mock(UserJpaRepository.class);
    private final TotpService totp = new TotpService();
    private final ArchbaseCryptoService crypto = new ArchbaseCryptoService("0123456789abcdef0123456789abcdef");
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final MfaService service = new MfaService(repository, totp, crypto, encoder);

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setEmail("user@vendax.com.br");
        when(repository.findById("u1")).thenReturn(Optional.of(user));
        when(repository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("iniciarConfiguracao persiste o segredo cifrado (não habilitado) e devolve a URI")
    void iniciar() {
        MfaSetup setup = service.iniciarConfiguracao("u1", "VendaX CRM");

        assertThat(setup.secret()).matches("[A-Z2-7]{32}");
        assertThat(setup.provisioningUri()).startsWith("otpauth://totp/");
        assertThat(user.getMfaSecret()).isNotNull().isNotEqualTo(setup.secret()); // cifrado em repouso
        assertThat(user.getMfaEnabled()).isFalse();
    }

    @Test
    @DisplayName("ativar rejeita código errado e, com o correto, habilita e gera 10 códigos de recuperação")
    void ativar() {
        MfaSetup setup = service.iniciarConfiguracao("u1", "VendaX CRM");

        assertThatThrownBy(() -> service.ativar("u1", "000000")).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getMfaEnabled()).isFalse();

        String codigo = totp.generateCode(setup.secret(), System.currentTimeMillis());
        List<String> recovery = service.ativar("u1", codigo);

        assertThat(recovery).hasSize(10).allMatch(c -> c.matches("[A-Z2-9]{5}-[A-Z2-9]{5}"));
        assertThat(user.getMfaEnabled()).isTrue();
        assertThat(user.getMfaRecoveryCodes()).isNotBlank();
    }

    @Test
    @DisplayName("verificar aceita TOTP válido e um código de recuperação (consumido no uso)")
    void verificar() {
        MfaSetup setup = service.iniciarConfiguracao("u1", "VendaX CRM");
        List<String> recovery = service.ativar("u1", totp.generateCode(setup.secret(), System.currentTimeMillis()));

        assertThat(service.verificar(user, totp.generateCode(setup.secret(), System.currentTimeMillis()))).isTrue();
        assertThat(service.verificar(user, "000000")).isFalse();

        String rc = recovery.get(0);
        assertThat(service.verificar(user, rc)).isTrue();  // usa o código de recuperação
        assertThat(service.verificar(user, rc)).isFalse(); // já consumido
    }

    @Test
    @DisplayName("usuário sem MFA passa direto; desativar limpa segredo e códigos")
    void semMfaEDesativar() {
        assertThat(service.verificar(user, null)).isTrue(); // MFA desabilitado → passa

        MfaSetup setup = service.iniciarConfiguracao("u1", "VendaX CRM");
        service.ativar("u1", totp.generateCode(setup.secret(), System.currentTimeMillis()));
        assertThat(user.getMfaEnabled()).isTrue();

        service.desativar("u1");
        assertThat(user.getMfaEnabled()).isFalse();
        assertThat(user.getMfaSecret()).isNull();
        assertThat(user.getMfaRecoveryCodes()).isNull();
    }
}
