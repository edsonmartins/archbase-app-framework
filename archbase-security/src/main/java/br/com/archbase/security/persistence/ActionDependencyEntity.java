package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.entity.DependencySource;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Uma aresta dirigida entre capacidades: <i>a origem não serve para nada sem o alvo</i>.
 *
 * <p>Cobre as duas perguntas que a tela de permissões não sabia responder — "do que esta permissão
 * depende?" e "qual endpoint esta tela usa?" —, porque as duas são a mesma aresta. Ver
 * {@code CONTRATO_DEPENDENCIAS_DE_CAPACIDADE.md}.
 *
 * <p><b>Informativa, nunca portão.</b> O {@code ArchbaseAccessEvaluator} não lê esta tabela, e não
 * há flag que o faça ler. Se {@code aprovar_custo} passasse a exigir {@code view} na decisão, toda
 * instalação existente perderia acesso na primeira subida após a atualização — em silêncio, porque
 * ninguém declarou aquelas arestas pensando em autorização.
 *
 * <p><b>Sem {@code @Audited}</b>, ao contrário de {@link ActionEntity} e {@link ResourceEntity}. A
 * trilha responde <i>quem mudou o acesso e quando</i>, e aresta não é mudada por gente: é mudada por
 * deploy. A fonte da verdade é o histórico do repositório.
 */
@Entity
@Getter
@Setter
@Table(name = "SEGURANCA_ACAO_DEPENDENCIA", uniqueConstraints =
        @UniqueConstraint(name = "uk_seguranca_acao_dependencia",
                columnNames = {"TENANT_ID", "ID_ACAO", "CAPACIDADE_REQUERIDA"}))
@AttributeOverrides({
        @AttributeOverride(name = "id",
                column = @Column(name = "ID_DEPENDENCIA", length = 40)),
        @AttributeOverride(name = "code",
                column = @Column(name = "CD_DEPENDENCIA", length = 40))
})
public class ActionDependencyEntity extends TenantPersistenceEntityBase {

    /** A capacidade que declara a dependência. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ACAO", nullable = false)
    private ActionEntity action;

    /**
     * O alvo, em <b>texto</b>: {@code recurso:acao}.
     *
     * <p>É a chave, e não o identificador, por duas razões que se somam. A varredura não tem ordem
     * garantida entre recursos — quando a aresta é gravada, a ação alvo pode ainda não existir — e
     * uma chave estrangeira obrigatória exigiria duas passadas ou ordenação artificial do catálogo.
     *
     * <p>Mais importante: o alvo <b>pode nunca existir</b>. Erro de digitação, módulo não implantado
     * naquele ambiente, recurso de tela que nenhum administrador abriu ainda. Nesses casos a aresta
     * precisa permanecer visível como não resolvida — sumir em silêncio é a classe de defeito que
     * zerou as ações de 56 dos 100 recursos do gestor-rq.
     */
    @Column(name = "CAPACIDADE_REQUERIDA", nullable = false, length = 200)
    private String requiredCapability;

    /**
     * O alvo resolvido — um espelho de {@link #requiredCapability}, não a chave.
     *
     * <p>Nulo enquanto a capacidade alvo não estiver no catálogo. É preenchido a cada sincronização,
     * o que faz uma aresta pendente passar a valer no dia em que o alvo é catalogado, sem ninguém
     * precisar reprocessar nada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ACAO_REQUERIDA", nullable = true)
    private ActionEntity requiredAction;

    /** Quem declarou — decide como a aresta é podada. */
    @Enumerated(EnumType.STRING)
    @Column(name = "DECLARADA_POR", nullable = false, length = 20)
    private DependencySource declaredBy;

    public ActionDependencyEntity() {
        super();
    }

    @Builder
    public ActionDependencyEntity(String id, String code, Long version, LocalDateTime createEntityDate,
                                  String createdByUser, LocalDateTime updateEntityDate,
                                  String lastModifiedByUser, String tenantId, ActionEntity action,
                                  String requiredCapability, ActionEntity requiredAction,
                                  DependencySource declaredBy) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.action = action;
        this.requiredCapability = requiredCapability;
        this.requiredAction = requiredAction;
        this.declaredBy = declaredBy;
    }

    /** {@code true} quando o alvo ainda não foi encontrado no catálogo. */
    public boolean isUnresolved() {
        return requiredAction == null;
    }
}
