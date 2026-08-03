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
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
        return grantedTo(subject, null);
    }

    /**
     * O que foi concedido ao sujeito, opcionalmente restrito a um recurso.
     *
     * <p>O filtro por recurso vai para a <b>consulta</b>, não para memória: a tela pergunta isto a
     * cada renderização, e carregar todas as concessões do usuário para descartar quase todas seria
     * trocar uma consulta filtrada por uma varredura.
     */
    @Transactional(readOnly = true)
    public List<EffectiveCapability> grantedTo(AccessSubject subject, String resourceName) {
        if (subject == null || subject.securityIds().isEmpty()) {
            return List.of();
        }

        List<PermissionEntity> permissoes = resourceName == null
                ? permissionRepository.findAllBySecurityIds(subject.securityIds())
                : permissionRepository.findAllBySecurityIdsAndResourceName(
                        subject.securityIds(), resourceName);
        // As capacidades negadas SEM RESTRIÇÃO DE ESCOPO. A negação vence a concessão, então uma
        // linha concedida por um grupo e negada no usuário não pode ser listada como efetiva —
        // seria a tela mostrando um botão que o backend recusa, e o diagnóstico contradizendo a
        // decisão que ele existe para explicar.
        //
        // Só as sem escopo, porém. Esta listagem é cega a tenant, empresa e projeto — sempre foi,
        // inclusive para concessões: ela responde "o que posso neste recurso", e a pergunta não
        // carrega escopo. Deixar uma negação RESTRITA suprimir a linha esconderia um botão que o
        // avaliador liberaria fora daquele escopo: divergência na direção oposta, e igualmente
        // errada. Quem precisa da resposta com escopo usa a simulação, que recebe os três campos.
        Set<String> negadas = new HashSet<>();
        for (PermissionEntity permissao : permissoes) {
            if (permissao.isDeny()
                    && permissao.semEstreitamentoDeEscopo()
                    && permissao.getAction() != null
                    && permissao.getAction().getResource() != null) {
                negadas.add(chave(permissao));
            }
        }

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

            EffectiveCapability.Situation situacao;
            // A propria linha de negacao NUNCA e concessao, tenha escopo ou nao. Sem esta
            // condicao, uma negacao estreitada por empresa nao entrava em `negadas` e caia no
            // ramo de "ativa" — sendo listada como EFFECTIVE. Bastava existir a negacao, sem
            // nenhuma concessao, para a tela renderizar o botao que o backend recusa.
            if (permissao.isDeny() || negadas.contains(chave(permissao))) {
                situacao = EffectiveCapability.Situation.DENIED;
            } else if (acaoAtiva && recursoAtivo) {
                situacao = EffectiveCapability.Situation.EFFECTIVE;
            } else {
                situacao = EffectiveCapability.Situation.INERT;
            }

            capacidades.add(new EffectiveCapability(
                    recurso.getName(),
                    acao.getName(),
                    destinatario == null ? null : destinatario.getId(),
                    destinatario == null ? null : destinatario.getName(),
                    tipoDe(destinatario),
                    acaoAtiva,
                    recursoAtivo,
                    situacao));
        }

        capacidades.sort(Comparator.comparing(EffectiveCapability::capability));
        return capacidades;
    }

    /** A capacidade que a linha aponta, para casar concessão com negação. */
    private String chave(PermissionEntity permissao) {
        return permissao.getAction().getResource().getName() + ":" + permissao.getAction().getName();
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
