package br.com.archbase.offline.sync.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Uma operação de mutação enviada pelo cliente no lote de sync.
 * Campos públicos para (de)serialização direta pelo Jackson.
 */
public class SyncOperationDTO {

    /** UUID da operação (gerado no cliente). É também a chave de idempotência. */
    public String id;

    /** Tipo de operação (ex.: {@code VISIT_CHECK_IN}) — casa com um handler. */
    public String type;

    /** Id do agregado de domínio afetado. */
    public String aggregateId;

    /** Corpo da operação (contrato específico do tipo). */
    public JsonNode payload;

    /** {@code serverVersion} conhecido pelo cliente (detecção de conflito). */
    public Long baseVersion;

    /** Id de outra operação do lote que deve concluir antes desta. */
    public String dependsOn;

    /** Instante de criação no cliente (ISO-8601). */
    public String clientCreatedAt;

    public SyncOperationDTO() {
    }
}
