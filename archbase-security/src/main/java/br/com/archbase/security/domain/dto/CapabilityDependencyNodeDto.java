package br.com.archbase.security.domain.dto;

import lombok.*;

/**
 * Uma capacidade alcançada pelo fecho de dependências, e por onde se chegou nela.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityDependencyNodeDto {

    /** A capacidade, em texto: {@code recurso:acao}. */
    private String capability;

    /**
     * O identificador da capacidade no catálogo, ou {@code null} quando ela <b>não existe</b>.
     *
     * <p>Nulo é um estado legítimo, não um erro: a dependência pode apontar para um módulo não
     * implantado naquele ambiente, ou para uma tela que nenhum administrador abriu ainda. A resposta
     * a devolve marcada em vez de omiti-la — omitir esconderia justamente o que precisa ser
     * corrigido.
     */
    private String actionId;

    private String resourceName;
    private String actionName;
    private String actionDescription;

    /** {@code 1} para as diretas; {@code 2} para as dependências das diretas, e assim por diante. */
    private int depth;

    /** A capacidade do nível anterior — o caminho até aqui, um passo por vez. */
    private String requiredBy;

    /** {@code false} quando {@link #actionId} é nulo. */
    private boolean resolved;
}
