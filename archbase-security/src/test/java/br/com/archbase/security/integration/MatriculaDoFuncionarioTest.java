package br.com.archbase.security.integration;

import br.com.archbase.security.adapter.UserPersistenceAdapter;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A matrícula do funcionário precisa atravessar o cadastro inteiro.
 *
 * <p><b>Por que um teste para um campo simples.</b> Um campo novo raramente falha ao ser declarado —
 * ele falha ao ser <b>esquecido</b> em um dos pontos da travessia. No cadastro de usuário são seis:
 * a coluna, o construtor da entidade, a conversão para o domínio, a volta do domínio, o DTO que
 * chega à tela e <b>a gravação de uma edição</b>. Esquecer qualquer um produz o mesmo sintoma: a
 * pessoa preenche o campo, salva, e o valor some sem erro nenhum.
 *
 * <p><b>A sexta passagem foi acrescentada depois, porque faltava — e o campo se perdia exatamente
 * ali.</b> A primeira versão deste teste cobria só as conversões e dava tudo verde, enquanto
 * {@code UserPersistenceAdapter.updateUser} atualizava campo a campo sem mencionar a matrícula.
 * Testar conversor não é testar o caminho que a aplicação percorre: o create copia o objeto inteiro
 * e passava, a edição não copiava e ninguém via.
 *
 * <p>Cada teste aqui cobre uma dessas passagens, indo e voltando.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste"
})
class MatriculaDoFuncionarioTest {

    private static final String EMAIL = "funcionario@vendax.com.br";
    private static final String MATRICULA = "RH-004521";

    @Autowired
    UserJpaRepository userRepository;

    @Autowired
    UserPersistenceAdapter adapter;

    @PersistenceContext
    EntityManager entityManager;

    @BeforeEach
    void limpar() {
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    /** O adapter registra quem alterou, então precisa de alguém autenticado. */
    private void autenticar() {
        UserEntity quemEdita = userRepository.saveAndFlush(UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Quem edita")
                .description("Quem edita")
                .email("editor@vendax.com.br")
                .userName("editor@vendax.com.br")
                .password("irrelevante")
                .isAdministrator(true)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .tenantId("tenant-teste")
                .build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(quemEdita, null, List.of()));
    }

    @Test
    @DisplayName("a matrícula é gravada e lida de volta do banco")
    void matriculaSobreviveAoBanco() {
        userRepository.saveAndFlush(usuarioCom(MATRICULA));

        UserEntity lido = userRepository.findByEmail(EMAIL).orElseThrow();

        assertThat(lido.getEmployeeId()).isEqualTo(MATRICULA);
    }

    @Test
    @DisplayName("a matrícula chega ao DTO, que é o que a tela recebe")
    void matriculaChegaAoDto() {
        // Entidade em memória de propósito: a conversão é o que está sendo testado, e uma entidade
        // recém-lida do banco traz coleções preguiçosas que estouram fora da sessão — ruído que
        // esconderia a falha real caso ela existisse.
        UserDto dto = usuarioCom(MATRICULA).toDto();

        // A travessia mais fácil de esquecer: o campo existe na entidade, mas a conversão para o DTO
        // não o copia. A tela mostra vazio e ninguém entende por quê.
        assertThat(dto.getEmployeeId()).isEqualTo(MATRICULA);
    }

    @Test
    @DisplayName("e volta do DTO para a entidade, que é o caminho de salvar")
    void matriculaVoltaDoDto() {
        UserDto dto = usuarioCom(MATRICULA).toDto();

        UserEntity deVolta = UserEntity.fromDomain(dto.toDomain());

        assertThat(deVolta.getEmployeeId()).isEqualTo(MATRICULA);
    }

    @Test
    @Transactional
    @DisplayName("editar um usuário grava a matrícula nova, e não a antiga")
    void matriculaAtravessaAAtualizacao() {
        autenticar();
        String id = userRepository.saveAndFlush(usuarioCom("RH-000001")).getId();

        UserDto alteracao = recarregar(id).toDto();
        alteracao.setEmployeeId(MATRICULA);
        adapter.updateUser(id, alteracao).orElseThrow();

        // Este é o caminho que a tela percorre ao salvar uma edição, e é onde o campo se perdia:
        // o create copia o objeto inteiro, mas a atualização é campo a campo e simplesmente não
        // mencionava a matrícula. A entidade vinha do banco com o valor antigo e voltava com ele,
        // então o que a pessoa digitava era descartado sem erro nenhum. As conversões testadas
        // acima passavam mesmo assim — elas não passam por aqui.
        assertThat(recarregar(id).getEmployeeId()).isEqualTo(MATRICULA);
    }

    @Test
    @Transactional
    @DisplayName("editar um usuário também grava o ID externo")
    void idExternoAtravessaAAtualizacao() {
        // Mesmo defeito, campo que já existia antes da matrícula: quem integra com Keycloak ou LDAP
        // não conseguia corrigir o vínculo pela tela. Corrigido junto por ser a mesma linha.
        autenticar();
        String id = userRepository.saveAndFlush(usuarioCom(MATRICULA)).getId();

        UserDto alteracao = recarregar(id).toDto();
        alteracao.setExternalId("keycloak-8842");
        adapter.updateUser(id, alteracao).orElseThrow();

        assertThat(recarregar(id).getExternalId()).isEqualTo("keycloak-8842");
    }

    /**
     * Esvazia a sessão antes de ler, para o valor vir do banco e não do cache de primeiro nível.
     * Sem o {@code clear()} a asserção leria a própria instância que o adapter acabou de alterar em
     * memória e passaria mesmo que nada tivesse sido gravado.
     */
    private UserEntity recarregar(String id) {
        entityManager.flush();
        entityManager.clear();
        return userRepository.findById(id).orElseThrow();
    }

    @Test
    @DisplayName("usuário sem matrícula continua válido")
    void matriculaEhOpcional() {
        // Controle: contas de serviço e usuários que não são funcionários não têm matrícula.
        // Exigi-la invalidaria todo cadastro que já existe.
        userRepository.saveAndFlush(usuarioCom(null));

        UserEntity lido = userRepository.findByEmail(EMAIL).orElseThrow();

        assertThat(lido.getEmployeeId()).isNull();
        assertThat(usuarioCom(null).toDto().getEmployeeId()).isNull();
    }

    private UserEntity usuarioCom(String matricula) {
        return UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Pessoa com matrícula")
                .description("Pessoa com matrícula")
                .email(EMAIL)
                .userName(EMAIL)
                .password("irrelevante")
                .employeeId(matricula)
                .isAdministrator(false)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .tenantId("tenant-teste")
                .build();
    }
}
