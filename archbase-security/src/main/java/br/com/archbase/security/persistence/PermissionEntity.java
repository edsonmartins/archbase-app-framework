package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.access.PermissionEffectConverter;
import br.com.archbase.security.access.PermissionEffect;
import br.com.archbase.security.domain.dto.PermissionDto;
import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.domain.dto.SecurityDto;
import br.com.archbase.security.domain.entity.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Auditada: a concessão em si: é a linha que decide quem pode o quê.
// A trilha só é gravada com archbase.security.audit.enabled=true e exige as tabelas _AUD.
@org.hibernate.envers.Audited
@Entity
@Getter
@Setter
@Table(name="SEGURANCA_PERMISSAO")
@AttributeOverrides({
        @AttributeOverride(name="id", column=@Column(name="ID_PERMISSAO", length = 40)),
        @AttributeOverride(name="code", column=@Column(name="CD_PERMISSAO", length = 40))
})
public class PermissionEntity extends TenantPersistenceEntityBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SEGURANCA", nullable = false)
    private SecurityEntity security;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ACAO", nullable = false)
    private ActionEntity action;
    // NÃO existe aqui um campo `tenantId` de escopo. Existiu, declarado com
    // @Column(name = "TENTANT_ID") — e nunca funcionou: o nome colidia com o discriminador de
    // tenant herdado de TenantPersistenceEntityBase, o Hibernate resolvia a ambiguidade em favor
    // do discriminador, e a coluna TENTANT_ID sequer chegava a ser criada. O getter devolvia o
    // tenant da linha, não um estreitamento — e allowAllTenantsAndCompaniesAndProjects(),
    // consultando-o, praticamente nunca devolvia true.
    //
    // O tenant continua disponível pelo pai, como discriminador. O estreitamento de uma permissão
    // é por EMPRESA e PROJETO; o isolamento entre tenants é do Hibernate, e não deste campo.
    @Column(name="COMPANY_ID", nullable = true)
    private String companyId;
    @Column(name="PROJECT_ID", nullable = true)
    private String projectId;

    /**
     * Se esta linha soma ou subtrai.
     *
     * <p>Nulo é {@link PermissionEffect#GRANT} — é o que toda concessão existente significa, e a
     * coluna nasce vazia para todas elas. {@code DENY} vence qualquer concessão de qualquer origem
     * dentro do mesmo escopo: é o que permite excluir uma pessoa de algo que o time inteiro tem,
     * sem criar um grupo paralelo só para isso.
     */
    @Convert(converter = PermissionEffectConverter.class)
    @Column(name="EFFECT", nullable = true, length = 10)
    private PermissionEffect effect;

    /** O efeito desta linha, com nulo resolvido para {@code GRANT}. */
    public PermissionEffect effectOrGrant() {
        return effect == null ? PermissionEffect.GRANT : effect;
    }

    public boolean isDeny() {
        return effectOrGrant() == PermissionEffect.DENY;
    }

    public PermissionEntity() {
        super();
    }

    @Builder
    public PermissionEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, SecurityEntity security, ActionEntity action, String companyId, String projectId, PermissionEffect effect) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.security = security;
        this.action = action;
        this.companyId = companyId;
        this.projectId = projectId;
        this.effect = effect;
    }

    public static PermissionEntity fromDomain(Permission permission) {
        if (permission == null) {
            return null;
        }
        SecurityEntity securityEntity = null;
        if (permission.getSecurity() != null) {
            if (permission.getSecurity() instanceof User) {
                securityEntity = UserEntity.fromDomain((User) permission.getSecurity());
            } else if (permission.getSecurity() instanceof Group){
                securityEntity = GroupEntity.fromDomain((Group) permission.getSecurity());
            } else if (permission.getSecurity() instanceof Profile) {
                securityEntity = ProfileEntity.fromDomain((Profile) permission.getSecurity());
            }
        }

        return PermissionEntity.builder()
                .id(permission.getId().toString())
                .code(permission.getCode())
                .version(permission.getVersion())
                .createEntityDate(permission.getCreateEntityDate())
                .updateEntityDate(permission.getUpdateEntityDate())
                .createdByUser(permission.getCreatedByUser())
                .lastModifiedByUser(permission.getLastModifiedByUser())
                .security(securityEntity)
                .action(ActionEntity.fromDomain(permission.getAction()))
                .effect(permission.getEffect())
                // O estreitamento era perdido aqui: as colunas chegavam nulas, e a permissão
                // passava a valer sem restrição. Para uma concessão isso já alargava o alcance em
                // silêncio; para uma NEGAÇÃO, transforma "bloquear na empresa A" em "bloquear em
                // todas".
                .companyId(permission.getCompanyId())
                .projectId(permission.getProjectId())
                .build();
    }

    public Permission toDomain() {
        Security security = null;
        if (this.security != null) {
            if (this.security instanceof UserEntity) {
                security = ((UserEntity) this.security).toDomain();
            } else if (this.security instanceof GroupEntity){
                security = ((GroupEntity) this.security).toDomain();
            } else if (this.security instanceof ProfileEntity) {
                security = ((ProfileEntity) this.security).toDomain();
            }
        }

        return Permission.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .security(security)
                .action(this.action.toDomain())
                .effect(this.effectOrGrant())
                .tenantId(this.getTenantId())
                .companyId(this.companyId)
                .projectId(this.projectId)
                .build();
    }

    public PermissionDto toDto() {
        SecurityDto security = null;
        if (this.security != null) {
            if (this.security instanceof UserEntity) {
                security = ((UserEntity) this.security).toDto();
            } else if (this.security instanceof GroupEntity){
                security = ((GroupEntity) this.security).toDto();
            } else if (this.security instanceof ProfileEntity) {
                security = ((ProfileEntity) this.security).toDto();
            }
        }

        return PermissionDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .security(security)
                .action(this.action.toDto())
                .effect(this.effectOrGrant())
                .tenantId(this.getTenantId())
                .companyId(this.companyId)
                .projectId(this.projectId)
                .build();
    }

    /**
     * {@code true} quando a permissão não estreita para empresa nem projeto.
     *
     * <p>Não consulta o tenant: ele é o <b>discriminador da linha</b>, preenchido em toda
     * permissão de uma aplicação multi-tenant. Incluí-lo aqui — como este método fazia enquanto
     * havia um campo {@code tenantId} sombreando o do pai — tornava o resultado praticamente
     * sempre {@code false}, ao contrário do que o nome prometia. O isolamento entre tenants é do
     * Hibernate; o que uma permissão estreita é empresa e projeto.
     */
    @JsonIgnore
    @Transient
    public boolean semEstreitamentoDeEscopo() {
        return companyId == null && projectId == null;
    }

    /**
     * @deprecated nome enganoso: nunca disse respeito a "todos os tenants", e consultava um campo
     *             que não era estreitamento. Use {@link #semEstreitamentoDeEscopo()}.
     */
    @Deprecated(forRemoval = true)
    @JsonIgnore
    @Transient
    public boolean allowAllTenantsAndCompaniesAndProjects() {
        return semEstreitamentoDeEscopo();
    }
}
