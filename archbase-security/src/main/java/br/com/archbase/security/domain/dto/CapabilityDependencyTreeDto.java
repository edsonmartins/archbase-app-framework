package br.com.archbase.security.domain.dto;

import lombok.*;

import java.util.List;

/**
 * Tudo de que uma capacidade depende, direta e indiretamente.
 *
 * <p>Resposta de {@code GET /api/v1/resource/permissions/dependencies/{actionId}}. Existe em
 * endpoint próprio, e não dentro do catálogo, porque o fecho de centenas de capacidades numa
 * resposta só custaria mais do que a informação vale — ele é olhado quando alguém abre o detalhe de
 * uma linha.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityDependencyTreeDto {

    private String actionId;

    /** A capacidade de partida, em texto. */
    private String capability;

    /**
     * O fecho <b>achatado</b>, ordenado por profundidade e depois por nome.
     *
     * <p>Achatado, e não aninhado, porque a mesma capacidade pode ser alcançada por mais de um
     * caminho: aninhar a repetiria em cada ramo, e a pergunta que a tela faz é "do que isto depende,
     * afinal" — uma lista, não uma árvore.
     */
    private List<CapabilityDependencyNodeDto> dependencies;

    /**
     * {@code true} quando o teto de profundidade foi atingido e há dependências não exploradas.
     *
     * <p>Dizer isso é o ponto. Uma resposta truncada em silêncio é pior que uma resposta ausente:
     * quem lê conclui que não há mais nada.
     */
    private boolean truncated;
}
