package br.com.archbase.security.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Uma revisão da trilha de auditoria: o "quando" e o "quem" de cada alteração.
 *
 * <p>O Envers grava, por padrão, apenas número e instante da revisão. Isso responde <i>o que
 * mudou</i> e deixa sem resposta a pergunta que motiva a trilha existir: <b>quem mudou</b>. As
 * colunas de autoria da entidade dizem quem alterou por último; a trilha precisa dizer quem fez
 * <i>cada</i> alteração, inclusive as que foram sobrescritas depois.
 *
 * <p>O tenant fica aqui, e não numa tabela por tenant. Separar em tabelas teria a vantagem de
 * isolar fisicamente, mas cobraria caro: cada consulta à trilha precisaria saber em qual tabela
 * olhar, e a numeração de revisão do Envers é global de qualquer forma. Com a coluna, o isolamento
 * passa a ser responsabilidade de quem lê — e quem lê é só administrador.
 */
@Entity
@Table(name = "SEGURANCA_REVISAO", indexes = {
        // A consulta natural da trilha é "o que aconteceu no tenant X neste período", e a purga
        // apaga por data. Sem estes índices, as duas varrem a tabela inteira.
        @Index(name = "IDX_SEGURANCA_REVISAO_TENANT", columnList = "TENANT_ID"),
        @Index(name = "IDX_SEGURANCA_REVISAO_DH", columnList = "DH_REVISAO")
})
@RevisionEntity(ArchbaseSecurityRevisionListener.class)
@Getter
@Setter
public class ArchbaseSecurityRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seguranca_revisao_seq")
    @SequenceGenerator(name = "seguranca_revisao_seq", sequenceName = "SEGURANCA_REVISAO_SEQ", allocationSize = 50)
    @RevisionNumber
    @Column(name = "ID_REVISAO")
    private long id;

    /**
     * Instante em milissegundos, exigência do contrato do Envers.
     *
     * <p>A coluna legível ao lado existe porque ninguém investiga incidente lendo epoch: quem abre
     * a tabela no banco precisa enxergar a data sem converter.
     */
    @RevisionTimestamp
    @Column(name = "DH_REVISAO_MILIS")
    private long timestamp;

    @Column(name = "DH_REVISAO")
    private java.time.LocalDateTime dataHora;

    /** Quem fez. Nulo quando a alteração não veio de uma requisição autenticada — carga, seed, job. */
    @Column(name = "USUARIO", length = 255)
    private String usuario;

    @Column(name = "TENANT_ID", length = 255)
    private String tenantId;

    /**
     * De onde veio a requisição.
     *
     * <p>Guardado com a mesma ressalva do resto do framework: atrás de proxy, o endereço só vale se
     * houver quem sobrescreva o cabeçalho — ver {@code archbase.security.client-ip.trust-forwarded-for}.
     */
    @Column(name = "ORIGEM", length = 100)
    private String origem;
}
