package br.com.archbase.security.integration;

import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.repository.ResourceJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O padrão é a trilha desligada, e isso não é preguiça: {@code @Audited} é anotação e não tem
 * interruptor. Uma vez nas entidades, o Envers passaria a gravar em toda aplicação que usa o
 * framework — e quem controla o schema por migrations não teria as tabelas {@code _AUD}. Atualizar
 * o framework quebraria a aplicação na primeira alteração de permissão, sem que ninguém tivesse
 * pedido auditoria.
 *
 * <p>Este teste é o que garante essa promessa: com a chave desligada, escrever nas entidades
 * anotadas funciona normalmente e <b>nenhuma tabela de auditoria é exigida</b>.
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
class TrilhaDesligadaPorPadraoTest {

    @Autowired
    ResourceJpaRepository resourceRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    @DisplayName("sem a chave, gravar entidade auditada não exige tabela de trilha")
    void gravaSemExigirTrilha() {
        ResourceEntity recurso = new ResourceEntity();
        recurso.setId(UUID.randomUUID().toString());
        recurso.setName("tms.ordemservico");
        recurso.setDescription("recurso de teste");
        recurso.setActive(true);
        recurso.setTenantId("tenant-teste");

        // Se o Envers estivesse ativo sem as tabelas, esta linha falharia.
        resourceRepository.saveAndFlush(recurso);

        assertThat(resourceRepository.findById(recurso.getId())).isPresent();
    }

    @Test
    @Transactional
    @DisplayName("com a trilha desligada, nada é gravado nas tabelas de auditoria")
    void naoGravaNaTrilha() {
        ResourceEntity recurso = new ResourceEntity();
        recurso.setId(UUID.randomUUID().toString());
        recurso.setName("compras.pedido");
        recurso.setDescription("recurso de teste");
        recurso.setActive(true);
        recurso.setTenantId("tenant-teste");
        resourceRepository.saveAndFlush(recurso);

        // Antes esta verificação era "select count(*) from SEGURANCA_RECURSO_AUD ... isZero()", o que
        // exigia que a tabela existisse. Ela não existe mais — e não existir é justamente a
        // correção. Com a trilha desligada não há tabela de auditoria alguma, o que torna
        // impossível gravar nelas.
        var consulta = entityManager.createNativeQuery(
                "select count(*) from information_schema.tables where upper(table_name) like '%\\_AUD' escape '\\'");

        assertThat(((Number) consulta.getSingleResult()).intValue()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("as duas tabelas que são entidades JPA seguem no mapeamento, e só elas")
    void apenasAsEntidadesJpaSeguemMapeadas() {
        // ESTE TESTE AFIRMAVA O CONTRÁRIO, e o comentário que o acompanhava dizia que as tabelas
        // _AUD ficarem no mapeamento "não é problema para quem usa migrations, porque sem gravação
        // a ausência delas nunca é percebida". A conclusão estava errada: quem sobe com
        // ddl-auto=validate percebe na hora, porque o Hibernate exige as tabelas e a aplicação não
        // sobe. Um consumidor real levou 177 testes de integração ao chão por isso.
        //
        // O teste consagrava o defeito, e foi por isso que ele durou. O que sobra agora são as duas
        // tabelas que não vêm do Envers: seguranca_revisao e seguranca_evento são @Entity comuns,
        // encontradas pelo @EntityScan da própria aplicação.
        var aud = entityManager.createNativeQuery(
                "select count(*) from information_schema.tables where upper(table_name) like '%\\_AUD' escape '\\'");
        assertThat(((Number) aud.getSingleResult()).intValue()).isZero();

        var entidades = entityManager.createNativeQuery(
                "select count(*) from information_schema.tables "
                        + "where upper(table_name) in ('SEGURANCA_REVISAO','SEGURANCA_EVENTO')");
        assertThat(((Number) entidades.getSingleResult()).intValue()).isEqualTo(2);
    }
}
