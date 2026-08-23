package br.com.archbase.ddd.domain.base;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base das entidades persistentes, com autoria preenchida pelo framework.
 *
 * <p><b>O {@code @EntityListeners} não é detalhe.</b> Sem ele, o {@code AuditorAware} que o
 * archbase-security registra e o {@code @EnableJpaAuditing} do starter ficam ligados e <b>nunca são
 * chamados</b> — nada aciona o listener. As colunas {@code USUARIO_CRIOU} e
 * {@code ULTIMO_USUARIO_ALTEROU} existiam há tempos e só continham o que a aplicação escrevesse à
 * mão, o que na prática significava vazio: era a auditoria que todo mundo supunha ter.
 *
 * <p>As datas seguem <b>sem</b> {@code @CreatedDate}/{@code @LastModifiedDate} de propósito. Muito
 * código já as atribui explicitamente, e anotá-las faria o listener sobrescrever esses valores na
 * gravação — mudança de comportamento que não tem a ver com o defeito sendo corrigido.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PersistenceEntityBase {

    @Id
    @Column(name="ID", length = 40)
    protected String id;

    @Column(name="CODIGO", length = 40)
    protected String code;

    @Version
    @Column(name="VERSAO")
    protected Long version;

    @Column(name="DH_CRIACAO")
    protected LocalDateTime createEntityDate;

    @CreatedBy
    @Column(name="USUARIO_CRIOU")
    protected String createdByUser;

    @Column(name="DH_ATUALIZACAO")
    protected LocalDateTime updateEntityDate;

    @LastModifiedBy
    @Column(name="ULTIMO_USUARIO_ALTEROU")
    protected String lastModifiedByUser;

    public PersistenceEntityBase() {
        this.id = UUID.randomUUID().toString();
        this.version = 1L;
        this.createEntityDate = LocalDateTime.now();
    }

    public PersistenceEntityBase(String id, String code) {
        this();
        if (id != null) {
            this.id = id;
        }
        this.code = code;
    }

    public PersistenceEntityBase(String code) {
        this();
        this.code = code;
    }

    public PersistenceEntityBase(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser) {
        this.id = id;
        this.code = code;
        this.version = version;
        this.createEntityDate = createEntityDate;
        this.createdByUser = createdByUser;
        this.updateEntityDate = updateEntityDate;
        this.lastModifiedByUser = lastModifiedByUser;
    }
}
