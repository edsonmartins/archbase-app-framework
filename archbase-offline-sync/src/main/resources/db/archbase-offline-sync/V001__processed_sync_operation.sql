-- Idempotência durável do protocolo batch de sync (archbase-offline-sync).
-- Copie/inclua em cada app que usa o módulo (ajuste a versão Flyway do app).
CREATE TABLE IF NOT EXISTS processed_sync_operation (
    tenant_id      VARCHAR(40)  NOT NULL,
    operation_id   VARCHAR(64)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    operation_type VARCHAR(80),
    aggregate_id   VARCHAR(64),
    server_version BIGINT,
    processed_at   TIMESTAMP    NOT NULL,
    CONSTRAINT pk_processed_sync_operation PRIMARY KEY (tenant_id, operation_id),
    CONSTRAINT ck_pso_status CHECK (status IN ('PROCESSED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_pso_processed_at
    ON processed_sync_operation (processed_at);
CREATE INDEX IF NOT EXISTS idx_pso_aggregate
    ON processed_sync_operation (tenant_id, aggregate_id);
