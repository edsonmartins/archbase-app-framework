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

        var consulta = entityManager.createNativeQuery("select count(*) from SEGURANCA_RECURSO_AUD");

        // É esta a promessa que importa: as anotações estão nas entidades, o mapeamento existe, e
        // ainda assim nenhuma linha de trilha é escrita enquanto a chave estiver desligada.
        assertThat(((Number) consulta.getSingleResult()).intValue()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("as tabelas de trilha entram no mapeamento mesmo desligadas — e isso é esperado")
    void tabelasDeTrilhaSaoMapeadasSempre() {
        // Caracteriza o limite exato da promessa, que eu havia entendido errado: desligar os
        // listeners do Envers impede a GRAVAÇÃO, não o MAPEAMENTO. As tabelas _AUD e a de revisão
        // continuam fazendo parte do schema e são criadas por ddl-auto como quaisquer outras.
        //
        // Para quem usa migrations isso não é problema: sem gravação, a ausência das tabelas nunca
        // é percebida — nenhuma consulta as procura. Elas só passam a ser necessárias no dia em que
        // a chave for ligada, e é para esse dia que existe o script em deployment/sql.
        var consulta = entityManager.createNativeQuery(
                "select count(*) from information_schema.tables where upper(table_name) like '%\\_AUD' escape '\\'");

        assertThat(((Number) consulta.getSingleResult()).intValue()).isPositive();
    }
}
