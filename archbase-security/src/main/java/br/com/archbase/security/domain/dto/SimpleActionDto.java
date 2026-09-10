package br.com.archbase.security.domain.dto;

import lombok.*;

import java.util.List;

/**
 * Uma capacidade declarada por uma tela, no {@code POST /api/v1/resource/register}.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleActionDto {

    private String actionName;

    private String actionDescription;

    /**
     * O rótulo curto — "Aprovar custo" —, distinto da descrição que explica a ação.
     *
     * <p>Nulo significa "use a descrição", que é o comportamento de todo cliente anterior. É semeado
     * apenas quando a capacidade ainda não tem rótulo: quem já tem não é sobrescrito, para o texto
     * não oscilar entre duas telas que registram a mesma ação.
     */
    private String actionLabel;

    /** O agrupamento dentro do recurso — "Custos". Mesma regra de semeadura do rótulo. */
    private String actionCategory;

    /**
     * As capacidades sem as quais este gesto da tela não funciona — tipicamente os endpoints que ele
     * chama.
     *
     * <p>Duas formas: {@code "view"} é a ação do <b>mesmo recurso</b> da tela;
     * {@code "tms.ordemservico:aprovar_custo"} é qualificada. É por aqui que a tela de permissões
     * passa a saber que o botão "Aprovar" do Cockpit depende de uma capacidade de API — e a exibir
     * as duas juntas em vez de como listas paralelas.
     *
     * <p><b>Nulo e lista vazia são coisas diferentes.</b> Nulo significa "não declarei" e não toca em
     * nada — é o que todo cliente anterior envia, e é o que preserva o comportamento atual. Lista
     * vazia significa "declaro que não há nenhuma", e remove as que existirem.
     *
     * <p><b>Informativo, nunca portão.</b> A decisão de acesso não lê estas arestas.
     */
    private List<String> requires;
}
