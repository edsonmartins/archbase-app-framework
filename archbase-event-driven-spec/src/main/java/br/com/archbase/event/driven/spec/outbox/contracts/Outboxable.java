package br.com.archbase.event.driven.spec.outbox.contracts;

/**
 * Descreve os principais atributos de um evento que deve
 * ser enviado além das fronteiras do serviço através da tabela "caixa de saída".
 *
 * @author edsonmartins
 */
public interface Outboxable {

    /**
     * O id do agregado afetado por um determinado evento. Isso também é usado para garantir
     * ordenação de eventos pertencentes a um mesmo agregado.
     */
    String getAggregateId();

    /**
     * O tipo de agregado afetado por um determinado evento. Este deve ser o mesmo tipo de string para todos
     * partes relacionadas de um mesmo agregado que podem ser alteradas.
     */
    String getAggregateType();

    /**
     * A carga útil do evento real como uma string JSON válida.
     */
    String getPayload();

    /**
     * O (sub) tipo de um evento real que causa qualquer alteração em um tipo de agregado específico.
     */
    String getType();

    /**
     * O registro de data e hora do aplicativo em que o evento ocorreu.
     */
    Long getTimestamp();
}