package br.com.archbase.event.driven.outbox;

import br.com.archbase.event.driven.spec.outbox.contracts.Outboxable;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import jakarta.validation.constraints.NotNull;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Evento gerado na caixa de saída e que será propagado para além das fronteiras
 * do serviço através da tabela "caixa de saída".
 */
@Entity
public class OutboxEvent {


    /**
     * id único de cada mensagem; pode ser usado pelos consumidores para detectar quaisquer eventos duplicados,
     * por exemplo, ao reiniciar para ler mensagens após uma falha. Gerado ao criar um novo evento.
     */
    @Id
    @GeneratedValue
    @Basic
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    /**
     * o tipo de raiz agregada à qual um determinado evento está relacionado; a ideia é, apoiando-se no mesmo conceito de
     * design orientado ao domínio, que os eventos exportados devem se referir a um agregado ( "um cluster de objetos de domínio
     * que pode ser tratado como uma única unidade" ), onde a raiz agregada fornece o único ponto de entrada para acessar
     * qualquer uma das entidades dentro do agregado. Pode ser, por exemplo, "pedido de compra" ou "cliente".
     * <p>
     * Este valor será usado para encaminhar eventos para tópicos correspondentes no Kafka, então haveria um tópico para
     * todos os eventos relacionados aos pedidos de compra, um tópico para todos os eventos relacionados ao cliente, etc.
     * Observe que também eventos pertencentes a uma entidade filha contida em um desses agregados deve usar o mesmo tipo.
     * Portanto, por exemplo, um evento que representa o cancelamento de uma linha de pedido individual (que faz parte do
     * agregado do pedido de compra) também deve usar o tipo de sua raiz agregada, "pedido", garantindo que esse evento
     * também vá para o tópico Kafka de "pedido" .
     */
    @NotNull
    private String aggregateType;

    /**
     * o id da raiz agregada que é afetada por um determinado evento; pode ser, por exemplo, o id de um pedido de compra
     * ou de um cliente; Semelhante ao tipo de agregado, os eventos pertencentes a uma subentidade contida em um agregado
     * devem usar o id da raiz agregada que contém, por exemplo, o id do pedido de compra para um evento de cancelamento
     * de linha de pedido. Este id será usado como chave para as mensagens Kafka mais tarde. Dessa forma, todos os eventos
     * pertencentes a uma raiz agregada ou qualquer uma de suas subentidades contidas irão para a mesma partição daquele
     * tópico Kafka, o que garante que os consumidores desse tópico consumirão todos os eventos relacionados a um e o mesmo
     * agregado no ordem exata em que foram produzidos.
     */
    @NotNull
    private String aggregateId;

    /**
     * o tipo de evento, por exemplo, "Pedido criado" ou "Linha de pedido cancelada". Permite que os consumidores acionem
     * manipuladores de eventos adequados.
     */
    @NotNull
    private String type;

    /**
     * Data/hora da geração do evento
     */
    @NotNull
    private Long timestamp;

    /**
     * uma estrutura JSON com o conteúdo real do evento, por exemplo, contendo um pedido de compra, informações sobre o
     * comprador, linhas de pedido contidas, seu preço etc.
     */
    @NotNull
    @Column(length = 1048576) //e.g. 1 MB max
    private String payload;

    private OutboxEvent() {
    }

    public OutboxEvent(String aggregateType, String aggregateId, String type, String payload, Long timestamp) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public static OutboxEvent from(Outboxable event) {
        return new OutboxEvent(
                event.getAggregateType(),
                event.getAggregateId(),
                event.getType(),
                event.getPayload(),
                event.getTimestamp()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
