package br.com.archbase.security.config;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A defesa nativa do Spring contra enumeração de usuários por tempo precisa ser <b>alcançada</b>.
 *
 * <p><b>O defeito que estes testes fixam.</b> {@code DaoAuthenticationProvider.retrieveUser} chama
 * {@code mitigateAgainstTimingAttack} — que confere a senha apresentada contra um hash fictício só
 * para gastar o mesmo tempo de bcrypt de um usuário que existe — e faz isso <b>exclusivamente</b>
 * dentro de {@code catch (UsernameNotFoundException)}. O bean do Archbase resolvia o usuário com
 * {@code Optional.get()}, que lança {@code NoSuchElementException}: essa exceção cai no
 * {@code catch (Exception)} seguinte e nunca chega à mitigação.
 *
 * <p>O efeito era invisível no corpo da resposta — e-mail inexistente e senha errada devolvem o
 * mesmo 401 "Login ou senha inválido" — e visível no relógio: sem bcrypt, o e-mail inexistente
 * respondia bem mais rápido, e a diferença de tempo entrega quem tem conta no sistema.
 *
 * <p>Os testes não medem tempo, que seria instável em CI. Medem o que causa o tempo: <b>quantas
 * vezes a comparação de senha é executada</b>. O último cenário reproduz o comportamento antigo e
 * prova que ele seria reprovado aqui.
 */
@DisplayName("Defesa de timing no login")
class DefesaDeTimingNoLoginTest {

    private static final String INEXISTENTE = "naoexiste@archbase.com.br";
    private static final String EXISTENTE = "existe@archbase.com.br";
    private static final String SENHA = "senha-correta";

    /** Conta as comparações de senha — é o custo que precisa ser igual nos dois caminhos. */
    private final AtomicInteger comparacoes = new AtomicInteger();

    private final PasswordEncoder encoderQueConta = new PasswordEncoder() {
        @Override
        public String encode(CharSequence senhaCrua) {
            return "hash::" + senhaCrua;
        }

        @Override
        public boolean matches(CharSequence senhaCrua, String hash) {
            comparacoes.incrementAndGet();
            return encode(senhaCrua).equals(hash);
        }
    };

    @Test
    @DisplayName("e-mail inexistente lança UsernameNotFoundException, não NoSuchElementException")
    void emailInexistenteLancaAExcecaoQueOSpringEntende() {
        UserDetailsService service = beanDeProducao(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(INEXISTENTE))
                .as("é esta exceção, e só esta, que faz o Spring pagar o bcrypt de mentira")
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("e-mail inexistente também paga a comparação de senha")
    void emailInexistentePagaAComparacao() {
        DaoAuthenticationProvider provider = provedorCom(beanDeProducao(Optional.empty()));

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken(INEXISTENTE, "qualquer")))
                .isInstanceOf(AuthenticationException.class);

        assertThat(comparacoes.get())
                .as("sem esta comparação, a resposta volta rápido demais e denuncia que o e-mail não existe")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("senha errada de usuário existente paga a mesma comparação — os dois custam igual")
    void senhaErradaPagaAMesmaComparacao() {
        DaoAuthenticationProvider provider = provedorCom(beanDeProducao(Optional.of(usuario())));

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken(EXISTENTE, "errada")))
                .isInstanceOf(AuthenticationException.class);

        assertThat(comparacoes.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("o comportamento anterior (Optional.get) seria reprovado: nenhuma comparação acontece")
    void comportamentoAnteriorNaoPagaNada() {
        // Reproduz literalmente o bean como era: Optional.get() em vazio lança NoSuchElementException.
        UserDetailsService beanAntigo = username -> Optional.<UserEntity>empty()
                .orElseThrow(NoSuchElementException::new);
        DaoAuthenticationProvider provider = provedorCom(beanAntigo);

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken(INEXISTENTE, "qualquer")))
                .isInstanceOf(AuthenticationException.class);

        assertThat(comparacoes.get())
                .as("prova de que este teste sabe falhar: era exatamente este zero que vazava o tempo")
                .isZero();
    }

    // ─────────────────────────── apoio ───────────────────────────

    /** O bean real de produção, com o repositório respondendo o que o cenário pedir. */
    private UserDetailsService beanDeProducao(Optional<UserEntity> resposta) {
        UserJpaRepository repository = Mockito.mock(UserJpaRepository.class);
        Mockito.when(repository.findByEmail(Mockito.anyString())).thenReturn(resposta);
        return new ArchbaseSecurityApplicationConfig(repository).userDetailsService();
    }

    private DaoAuthenticationProvider provedorCom(UserDetailsService service) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(encoderQueConta);
        return provider;
    }

    private UserEntity usuario() {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Usuário")
                .description("Usuário")
                .email(EXISTENTE)
                .userName(EXISTENTE)
                .password(encoderQueConta.encode(SENHA))
                .isAdministrator(false)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .build();
        user.setTenantId("tenant-teste");
        return user;
    }
}
