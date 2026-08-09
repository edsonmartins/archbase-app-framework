package br.com.archbase.security.integration;

import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.repository.ResourceJpaRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A trilha existe para responder <b>quem</b> mudou uma permissão, e <b>quando</b> — a pergunta que
 * aparece quando alguém ganhou um acesso que não deveria ter.
 *
 * <p>As colunas de autoria da entidade não respondem isso: elas guardam apenas o último autor, e
 * quem investiga precisa justamente das alterações que foram sobrescritas depois.
 *
 * <p>Este teste sobe com a trilha <b>ligada</b>. O caso contrário — o padrão, desligado — é o de
 * {@link TrilhaDesligadaPorPadraoTest}, e é o que garante que atualizar o framework não obrigue
 * ninguém a criar tabela nenhuma.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.audit.enabled=true",
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
class TrilhaDeAuditoriaTest {

    @Autowired
    ResourceJpaRepository resourceRepository;

    @Autowired
    EntityManager entityManager;

    /**
     * As escritas precisam <b>commitar</b>.
     *
     * <p>O Envers materializa a revisão no fim da transação, não no flush. Um teste anotado com
     * {@code @Transactional} lê dentro da mesma transação que escreveu e encontra a trilha vazia —
     * o que parece defeito da auditoria e é só ordem dos acontecimentos.
     */
    @Autowired
    TransactionTemplate transacao;

    @Test
    @DisplayName("alterar um recurso deixa rastro: cada versão vira uma revisão")
    void alteracaoGeraRevisao() {
        ResourceEntity recurso = novoRecurso("tms.ordemservico");
        transacao.executeWithoutResult(t -> resourceRepository.saveAndFlush(recurso));

        transacao.executeWithoutResult(t -> {
            ResourceEntity salvo = resourceRepository.findById(recurso.getId()).orElseThrow();
            salvo.setDescription("descrição corrigida");
            resourceRepository.saveAndFlush(salvo);
        });

        List<Number> revisoes = transacao.execute(t -> AuditReaderFactory.get(entityManager)
                .getRevisions(ResourceEntity.class, recurso.getId()));

        // Duas escritas, duas revisões: a trilha guarda o estado anterior, não só o atual.
        assertThat(revisoes).hasSize(2);
    }

    @Test
    @DisplayName("a revisão guarda o instante e o tenant de quem alterou")
    void revisaoGuardaQuemEQuando() {
        // Fora de uma requisição HTTP ninguém preenche o contexto de tenant — o filtro é quem faz
        // isso. Pôr à mão aqui é o que torna o teste uma verificação do listener, e não do filtro.
        br.com.archbase.ddd.context.ArchbaseTenantContext.setTenantId("tenant-teste");
        try {
            ResourceEntity recurso = novoRecurso("financeiro.pagamento");
            transacao.executeWithoutResult(t -> resourceRepository.saveAndFlush(recurso));

            var dados = transacao.execute(t -> {
                Number revisao = AuditReaderFactory.get(entityManager)
                        .getRevisions(ResourceEntity.class, recurso.getId())
                        .get(0);
                return AuditReaderFactory.get(entityManager)
                        .findRevision(br.com.archbase.security.persistence.ArchbaseSecurityRevision.class, revisao);
            });

            assertThat(dados.getDataHora()).isNotNull();
            // Sem autenticação o usuário fica nulo de propósito — seed e job alteram segurança sem
            // usuário, e inventar um nome ali seria pior que admitir a ausência.
            assertThat(dados.getUsuario()).isNull();
            assertThat(dados.getTenantId()).isEqualTo("tenant-teste");
        } finally {
            br.com.archbase.ddd.context.ArchbaseTenantContext.clear();
        }
    }

    @Test
    @DisplayName("o estado anterior continua legível depois de sobrescrito")
    void estadoAnteriorSobrevive() {
        ResourceEntity recurso = novoRecurso("compras.pedido");
        recurso.setDescription("como era antes");
        transacao.executeWithoutResult(t -> resourceRepository.saveAndFlush(recurso));

        transacao.executeWithoutResult(t -> {
            ResourceEntity salvo = resourceRepository.findById(recurso.getId()).orElseThrow();
            salvo.setDescription("como ficou depois");
            resourceRepository.saveAndFlush(salvo);
        });

        ResourceEntity primeira = transacao.execute(t -> {
            var leitor = AuditReaderFactory.get(entityManager);
            List<Number> revisoes = leitor.getRevisions(ResourceEntity.class, recurso.getId());
            return leitor.find(ResourceEntity.class, recurso.getId(), revisoes.get(0));
        });

        // É isto que as colunas de autoria não dão: o valor que existia antes da última alteração.
        assertThat(primeira.getDescription()).isEqualTo("como era antes");
    }

    private ResourceEntity novoRecurso(String nome) {
        ResourceEntity recurso = new ResourceEntity();
        recurso.setId(UUID.randomUUID().toString());
        recurso.setName(nome);
        recurso.setDescription("recurso de teste");
        recurso.setActive(true);
        recurso.setTenantId("tenant-teste");
        return recurso;
    }
}
