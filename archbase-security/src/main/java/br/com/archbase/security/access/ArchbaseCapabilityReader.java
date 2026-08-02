package br.com.archbase.security.access;

import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.SecurityEntity;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lista o que foi concedido a um sujeito — <b>com a origem junto</b>.
 *
 * <p>Existe para que só haja <b>uma</b> resposta a essa pergunta no framework. Antes havia duas
 * implementações independentes: a JPQL que o {@code @HasPermission} usa e a QueryDSL do
 * {@code ResourcePersistenceAdapter}, que o frontend consome. Além de duplicadas, divergiam — a do
 * frontend filtrava {@code action.active}, a do backend não. Duas respostas para a mesma pergunta é
 * como um sistema acaba concedendo mais do que a interface mostra.
 *
 * <p>Listar não é decidir. Aqui não há atalho de administrador nem avaliação de portão: a lista
 * mostra o que o catálogo registra. Quem decide é o {@link ArchbaseAccessEvaluator}.
 */
@Component
public class ArchbaseCapabilityReader {

    private final PermissionJpaRepository permissionRepository;

    public ArchbaseCapabilityReader(PermissionJpaRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /** Tudo que foi concedido ao sujeito, por qualquer das suas origens. */
    @Transactional(readOnly = true)
    public List<EffectiveCapability> grantedTo(AccessSubject subject) {
        if (subject == null || subject.securityIds().isEmpty()) {
            return List.of();
        }

        List<PermissionEntity> permissoes = permissionRepository.findAllBySecurityIds(subject.securityIds());
        List<EffectiveCapability> capacidades = new ArrayList<>(permissoes.size());

        for (PermissionEntity permissao : permissoes) {
            ActionEntity acao = permissao.getAction();
            if (acao == null || acao.getResource() == null) {
                continue;
            }
            ResourceEntity recurso = acao.getResource();
            boolean acaoAtiva = Boolean.TRUE.equals(acao.getActive());
            boolean recursoAtivo = Boolean.TRUE.equals(recurso.getActive());
            SecurityEntity destinatario = permissao.getSecurity();

            capacidades.add(new EffectiveCapability(
                    recurso.getName(),
                    acao.getName(),
                    destinatario == null ? null : destinatario.getId(),
                    destinatario == null ? null : destinatario.getName(),
                    tipoDe(destinatario),
                    acaoAtiva,
                    recursoAtivo,
                    acaoAtiva && recursoAtivo
                            ? EffectiveCapability.Situation.EFFECTIVE
                            : EffectiveCapability.Situation.INERT));
        }

        capacidades.sort(Comparator.comparing(EffectiveCapability::capability));
        return capacidades;
    }

    /**
     * O discriminador da hierarquia de {@code SecurityEntity} — {@code User}, {@code Group} ou
     * {@code Profile}.
     *
     * <p>Passa por {@code Hibernate.getClass} porque o destinatário costuma chegar como proxy, e o
     * nome da classe do proxy não é o da entidade.
     */
    private String tipoDe(SecurityEntity destinatario) {
        if (destinatario == null) {
            return null;
        }
        return Hibernate.getClass(destinatario).getSimpleName().replace("Entity", "");
    }
}
