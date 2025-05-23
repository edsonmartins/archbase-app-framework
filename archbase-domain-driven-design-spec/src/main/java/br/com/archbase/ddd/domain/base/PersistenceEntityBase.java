package br.com.archbase.ddd.domain.base;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public class PersistenceEntityBase {

    @Id
    @Column(name="ID")
    protected String id;

    @Column(name="CODIGO")
    protected String code;

    @Version
    @Column(name="VERSAO")
    protected Long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="DH_CRIACAO")
    protected LocalDateTime createEntityDate;

    @Column(name="USUARIO_CRIOU")
    protected String createdByUser;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="DH_ATUALIZACAO")
    protected LocalDateTime updateEntityDate;

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
