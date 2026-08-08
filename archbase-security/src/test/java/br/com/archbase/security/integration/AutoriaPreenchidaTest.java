package br.com.archbase.security.integration;

import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.ResourceJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As colunas de autoria precisam ser preenchidas pelo framework.
 *
 * <p><b>O defeito que este teste fixa.</b> {@code USUARIO_CRIOU} e {@code ULTIMO_USUARIO_ALTEROU}
 * existiam há tempos em {@code PersistenceEntityBase}, o {@code AuditorAware} era registrado pelo
 * archbase-security e o {@code @EnableJpaAuditing} estava ativo no starter — mas <b>nada acionava o
 * listener</b>, porque faltava o {@code @EntityListeners} na entidade base. Resultado: as colunas só
 * continham o que a aplicação escrevesse à mão, o que na prática significava vazio.
 *
 * <p>Era a auditoria que todos supunham existir. A pergunta "quem criou este registro?" não tinha
 * resposta, e ninguém percebia porque a coluna estava lá.
 *
 * <p>Escondido atrás desse, um segundo defeito: o {@code ApplicationAuditAware} fazia
 * {@code (User) getPrincipal()}, e o principal do filtro JWT é um {@link UserEntity} — que não é um
 * {@code domain.entity.User}. Se o listener tivesse sido acionado, toda escrita autenticada teria
 * terminado em {@code ClassCastException}.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-autoria-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Autoria preenchida pelo framework (Spring + H2)")
class AutoriaPreenchidaTest {

    @Autowired
    ResourceJpaRepository resourceRepository;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    AuditorAware<String> auditorAware;

    @BeforeEach
    void limpar() {
        resourceRepository.deleteAll();
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("grava quem criou o registro, a partir do usuário autenticado")
    void gravaQuemCriou() {
        UserEntity autor = autenticar();

        ResourceEntity salvo = resourceRepository.save(ResourceEntity.builder()
                .id("res-1").name("tms.ordemservico").description("Ordem de serviço").active(true)
                .createEntityDate(LocalDateTime.now()).build());

        // Antes da correção este campo vinha nulo, porque o listener nunca era acionado.
        assertThat(salvo.getCreatedByUser())
                .as("sem isto, a pergunta 'quem criou este registro?' não tem resposta")
                .isEqualTo(autor.getId());
    }

    @Test
    @DisplayName("grava quem alterou por último")
    void gravaQuemAlterou() {
        autenticar();
        ResourceEntity salvo = resourceRepository.save(ResourceEntity.builder()
                .id("res-2").name("tms.pneu").description("Pneus").active(true)
                .createEntityDate(LocalDateTime.now()).build());

        UserEntity outro = autenticarComo("segunda-pessoa");
        salvo.setDescription("Pneus e rodas");
        ResourceEntity alterado = resourceRepository.save(salvo);

        assertThat(alterado.getLastModifiedByUser()).isEqualTo(outro.getId());
    }

    @Test
    @DisplayName("sem ninguém autenticado, a autoria fica vazia — e a gravação acontece")
    void semAutenticadoNaoQuebra() {
        // Seed, migração, job agendado: gravam sem contexto de segurança. Auditoria em branco é
        // aceitável; gravação perdida não é.
        ResourceEntity salvo = resourceRepository.save(ResourceEntity.builder()
                .id("res-3").name("relatorio").description("Relatório").active(true)
                .createEntityDate(LocalDateTime.now()).build());

        assertThat(salvo.getCreatedByUser()).isNull();
    }

    @Test
    @DisplayName("principal de UserDetailsService próprio não derruba a gravação")
    void principalDesconhecidoNaoQuebra() {
        // O framework suporta oficialmente UserDetailsService próprio — o bean dele é
        // @ConditionalOnMissingBean. Nesse caso o principal NÃO é um UserEntity, e era exatamente
        // aí que o cast antigo estouraria.
        UserDetails proprio = new UserDetails() {
            @Override public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public String getPassword() { return "irrelevante"; }
            @Override public String getUsername() { return "pessoa@aplicacao.com.br"; }
        };
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(proprio, null, List.of()));

        ResourceEntity salvo = resourceRepository.save(ResourceEntity.builder()
                .id("res-4").name("tela.propria").description("Tela").active(true)
                .createEntityDate(LocalDateTime.now()).build());

        // Não sabemos o id, mas o login é identificador suficiente — e melhor que perder a operação.
        assertThat(salvo.getCreatedByUser()).isEqualTo("pessoa@aplicacao.com.br");
        assertThat(auditorAware.getCurrentAuditor()).contains("pessoa@aplicacao.com.br");
    }

    private UserEntity autenticar() {
        return autenticarComo("primeira-pessoa");
    }

    private UserEntity autenticarComo(String id) {
        UserEntity user = userRepository.save(UserEntity.builder()
                .id(id)
                .name("Pessoa " + id)
                .description("Pessoa de teste")
                .userName(id)
                .email(id + "@exemplo.test")
                .password("irrelevante")
                .isAdministrator(false)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .createEntityDate(LocalDateTime.now())
                .build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        return user;
    }
}
