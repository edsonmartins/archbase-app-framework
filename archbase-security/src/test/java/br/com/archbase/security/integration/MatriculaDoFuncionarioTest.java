package br.com.archbase.security.integration;

import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A matrícula do funcionário precisa atravessar o cadastro inteiro.
 *
 * <p><b>Por que um teste para um campo simples.</b> Um campo novo raramente falha ao ser declarado —
 * ele falha ao ser <b>esquecido</b> em um dos pontos da travessia. No cadastro de usuário são cinco:
 * a coluna, o construtor da entidade, a conversão para o domínio, a volta do domínio e o DTO que
 * chega à tela. Esquecer qualquer um produz o mesmo sintoma: a pessoa preenche o campo, salva, e o
 * valor some sem erro nenhum.
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

    @BeforeEach
    void limpar() {
        userRepository.deleteAll();
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

        assertThat(deVolta.getEmployeeId())
                .as("sem isto, editar um usuário pela tela apagaria a matrícula silenciosamente")
                .isEqualTo(MATRICULA);
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
