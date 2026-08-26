package br.com.archbase.security.integration;

import br.com.archbase.security.audit.SecurityEventType;
import br.com.archbase.security.persistence.SecurityEventEntity;
import br.com.archbase.security.repository.SecurityEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A trilha de um tenant não aparece para outro.
 *
 * <p>Este teste existe porque o defeito que ele cobre passou por uma auditoria inteira sem ser
 * notado, e o motivo é instrutivo: {@code LeituraDaTrilhaTest} verifica com cuidado <b>quem</b> lê a
 * trilha — administrador sim, usuário comum não, anônimo não —, mas faz tudo dentro de um único
 * tenant. Onde só existe um tenant, vazamento entre tenants não tem como aparecer.
 *
 * <p>O que estava errado: {@code SecurityEventEntity} é a única das quinze entidades do módulo que
 * não estende {@code TenantPersistenceEntityBase}, e portanto a única sem o {@code @TenantId} que
 * faz o Hibernate filtrar sozinho. O logger lia o {@code ArchbaseTenantContext} e gravava a coluna
 * à mão — a intenção de isolar estava lá —, mas a consulta de leitura não a usava. Um administrador
 * de um tenant lia os eventos de todos os outros.
 *
 * <p>Exercita o repositório em vez do endpoint porque é na consulta que o filtro vive; a trava de
 * administrador do controller já é coberta pelo outro teste.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.audit.enabled=true",
        "archbase.security.audit.tenant-scoped=true",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-a"
})
class TrilhaNaoVazaEntreTenantsTest {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";

    @Autowired
    SecurityEventJpaRepository eventRepository;

    @BeforeEach
    void gravarEventosDosDoisTenants() {
        eventRepository.deleteAll();
        eventRepository.saveAll(List.of(
                evento("alice@tenant-a.com", TENANT_A),
                evento("bob@tenant-a.com", TENANT_A),
                evento("carol@tenant-b.com", TENANT_B)));
    }

    @Test
    @DisplayName("com escopo por tenant, a consulta devolve só os eventos do tenant pedido")
    void naoEnxergaOOutroTenant() {
        var doA = buscar(TENANT_A);

        assertThat(doA).hasSize(2);
        assertThat(doA).extracting(SecurityEventEntity::getTenantId).containsOnly(TENANT_A);
        assertThat(doA).extracting(SecurityEventEntity::getUsuario)
                .doesNotContain("carol@tenant-b.com");
    }

    @Test
    @DisplayName("cada tenant enxerga os próprios eventos, e só")
    void cadaUmVeOSeu() {
        assertThat(buscar(TENANT_B)).extracting(SecurityEventEntity::getUsuario)
                .containsExactly("carol@tenant-b.com");
    }

    @Test
    @DisplayName("sem tenant, a consulta segue global — é o que a purga e o console de suporte usam")
    void semTenantNaoEstreita() {
        // String vazia é a convenção de "não filtre", a mesma que o filtro de usuário já usava.
        // Preserva o comportamento de quem mantém archbase.security.audit.tenant-scoped=false.
        assertThat(buscar("")).hasSize(3);
    }

    private List<SecurityEventEntity> buscar(String tenant) {
        return eventRepository.buscar(
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                null, "", tenant,
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
    }

    private static SecurityEventEntity evento(String usuario, String tenant) {
        return SecurityEventEntity.builder()
                .id(UUID.randomUUID().toString())
                .tipo(SecurityEventType.LOGIN)
                .dataHora(LocalDateTime.now())
                .usuario(usuario)
                .tenantId(tenant)
                .sucesso(true)
                .build();
    }
}
