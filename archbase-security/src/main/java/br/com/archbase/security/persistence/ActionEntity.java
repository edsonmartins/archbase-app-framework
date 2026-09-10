package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.access.AccessLevelConverter;
import br.com.archbase.security.access.AccessLevel;
import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.domain.entity.Action;
import br.com.archbase.security.domain.entity.Resource;
import br.com.archbase.shared.kernel.converters.BooleanToSNConverter;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Auditada: o catálogo do que existe para ser concedido.
// A trilha só é gravada com archbase.security.audit.enabled=true e exige as tabelas _AUD.
@org.hibernate.envers.Audited
@Entity
@Getter
@Setter
/**
 * Uma capacidade do catálogo.
 *
 * <p>A restrição de unicidade em {@code (TENANT_ID, ID_RECURSO, NOME)} existe porque duas ações de
 * mesmo nome sob o mesmo recurso tornam a autorização ambígua: as duas casam a consulta de decisão,
 * e os pisos podem divergir. Vale para schemas gerados a partir das entidades. Em bancos já
 * existentes ela <b>não</b> é aplicada pela migration — criar o índice sobre dados duplicados
 * falharia, e derrubar a subida de quem já tem o problema seria trocar uma ambiguidade por uma
 * indisponibilidade. O validador de subida reporta as duplicatas para que sejam resolvidas antes.
 *
 * <p><b>Alcance real da restrição.</b> {@code TENANT_ID} é nulável, e PostgreSQL trata NULLs como
 * distintos num índice único: numa instalação sem multi-tenancy, onde toda ação tem
 * {@code tenant_id IS NULL}, a restrição <b>não impede</b> a duplicata. Fechar isso exige
 * {@code NULLS NOT DISTINCT} (PostgreSQL 15+) ou um índice parcial, o que sai do que o mapeamento
 * JPA consegue expressar. Por isso o aviso do validador não é redundante com a constraint: nas
 * instalações single-tenant ele é a <b>única</b> proteção.
 */
@Table(name="SEGURANCA_ACAO", uniqueConstraints =
        @UniqueConstraint(name = "uk_seguranca_acao_recurso_nome",
                columnNames = {"TENANT_ID", "ID_RECURSO", "NOME"}))
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_ACAO", length = 40)),
        @AttributeOverride(name="code",
                column=@Column(name="CD_ACAO", length = 40))
})
public class ActionEntity extends TenantPersistenceEntityBase {

    @Column(name = "NOME", nullable = false)
    private String name;

    @Column(name = "DESCRICAO", nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "ID_RECURSO", nullable = false)
    private ResourceEntity resource;

    /**
     * O rótulo curto — "Aprovar custo" —, distinto da descrição, que explica <b>o que a ação faz</b>.
     *
     * <p>Nulo é o estado de toda ação existente, e significa "use a descrição". Não há backfill: as
     * descrições atuais funcionam como rótulo hoje, e reescrevê-las em massa trocaria um texto que
     * alguém conhece por outro que ninguém pediu. A separação passa a valer para quem declarar
     * {@code @HasPermission(label = ...)}, e o resto segue exatamente como está.
     *
     * <p>Semeado no primeiro registro, como a descrição e o nível mínimo — a partir daí quem manda é
     * o admin. {@code archbase.security.sync.mode=refresh} é o que ressemeia a partir do código.
     */
    @Column(name = "ROTULO", nullable = true, length = 120)
    private String label;

    /**
     * O eixo de agrupamento das capacidades dentro do recurso — "Custos", "Faturamento".
     *
     * <p>A coluna existe desde sempre e <b>nunca foi preenchida</b> por nenhum coletor. Enquanto
     * isso, o agrupamento era improvisado dentro da descrição, com um {@code ->} que o cliente
     * quebra na exibição — de modo que o mesmo campo identificava, explicava e agrupava.
     */
    @Column(name = "CATEGORIA", nullable = true)
    private String category;

    @Column(name = "BO_ATIVA", nullable = false, length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean active;

    @Column(name = "VERSAO_ACAO", nullable = true)
    private String actionVersion;

    /**
     * O nível mínimo que esta capacidade exige — o piso do portão {@code LEVEL}.
     *
     * <p>Nulo significa <b>sem piso</b>, e é como toda ação existente nasce: a coluna entra vazia e
     * o portão passa direto. O valor é semeado pelo código, em
     * {@code @HasPermission(minimumLevel = ...)}, no primeiro registro da ação; a partir daí quem
     * manda é o admin, igual já acontece com a descrição.
     */
    @Convert(converter = AccessLevelConverter.class)
    @Column(name = "MINIMUM_LEVEL", nullable = true, length = 30)
    private AccessLevel minimumLevel;

    public ActionEntity() {
        super();
    }

    @Builder
    public ActionEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description, ResourceEntity resource, String label, String category, Boolean active, String actionVersion, AccessLevel minimumLevel) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.label = label;
        this.category = category;
        this.active = active;
        this.actionVersion = actionVersion;
        this.minimumLevel = minimumLevel;
    }

    public static ActionEntity fromDomain(Action action) {
        if (action == null) {
            return null;
        }

        ActionEntity actionEntity = new ActionEntity();
        actionEntity.setId(action.getId().toString());
        actionEntity.setCode(action.getCode());
        actionEntity.setVersion(action.getVersion());
        actionEntity.setName(action.getName());
        actionEntity.setDescription(action.getDescription());
        actionEntity.setResource(ResourceEntity.fromDomain(action.getResource()));
        actionEntity.setLabel(action.getLabel());
        actionEntity.setCategory(action.getCategory());
        actionEntity.setMinimumLevel(action.getMinimumLevel());
        actionEntity.setActive(action.getActive());
        actionEntity.setActionVersion(action.getActionVersion());
        return actionEntity;
    }

    public Action toDomain() {
        Resource resourceDomain = this.resource != null ? this.resource.toDomain() : null;

        return Action.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .resource(resourceDomain)
                .label(this.getLabel())
                .category(this.getCategory())
                .active(this.getActive())
                .actionVersion(this.getActionVersion())
                .minimumLevel(this.getMinimumLevel())
                .build();
    }

    public ActionDto toDto() {
        ResourceDto resourceDto = this.resource != null ? this.resource.toDto() : null;

        return ActionDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .resource(resourceDto)
                .label(this.getLabel())
                .category(this.getCategory())
                .active(this.getActive())
                .actionVersion(this.getActionVersion())
                .minimumLevel(this.getMinimumLevel())
                .build();
    }
}
