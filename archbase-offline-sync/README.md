# archbase-offline-sync

Backend base do protocolo **batch offline-first** para apps Flutter que usam o
módulo `archbase_offline` do `archbase_flutter`. Ver o contrato em
`archbase-flutter/docs/design/archbase-offline-module.md` e a decisão em
`archbase-flutter/docs/adr/ADR-0001-motor-offline-first.md`.

## O que entrega
- **Endpoints** `POST /api/v1/sync/operations` → `SyncOperationProcessor.process(request)` e
  `POST /api/v1/sync/operations/status`, no `ArchbaseSyncController` — registrado pelo
  [`archbase-starter-offline-sync`](../archbase-starter-offline-sync/README.md).
- **Processamento por operação** com **transação própria** (`REQUIRES_NEW`):
  uma falha/conflito reverte só aquela operação, nunca o lote — o ACK é individual.
- **Idempotência durável** (`processed_sync_operation`): reenvio não reexecuta (→ SKIPPED).
- **Conflito tipado** (`SyncConflictException` → CONFLICT), **skip** (SKIPPED),
  **tipo desconhecido** (REJECTED), **dependências** no lote (BLOCKED) e
  **transitório** (FAILED, o cliente reenvia).

- **`payload` como `JsonNode` do Jackson 2** (`com.fasterxml.jackson.databind.JsonNode`), lido também pelo Jackson 3
  que o Spring MVC do Boot 4 usa no corpo da requisição. Até a 3.5.0 o Jackson 3 não construía esse tipo e todo lote
  com `payload` era recusado com 400 antes de chegar ao handler.

## Como o app usa

### Com o starter (recomendado)

1. Adicione `br.com.archbase:archbase-starter-offline-sync`. Ele registra o processador, o executor, o
   `ArchbaseSyncController` (as duas rotas acima), a limpeza `ProcessedSyncOperationCleanup`, a entidade
   `ProcessedSyncOperation` e o repositório dela — somados aos da aplicação. **Não escreva outro controller em
   `/api/v1/sync/operations`**: o mapeamento duplicado impede a subida.
2. Implemente `SyncTenantProvider` (delegue ao multitenancy do Archbase).
3. Para cada tipo de operação, um bean `SyncOperationHandler`:
   ```java
   @Component
   class VisitCheckInHandler implements SyncOperationHandler {
     public String type() { return "VISIT_CHECK_IN"; }
     public SyncHandlerResult handle(SyncOperationDTO op) {
       // ler op.payload (JsonNode), aplicar; lançar SyncConflictException/
       // SyncSkippedException conforme o caso; devolver o serverVersion.
       return SyncHandlerResult.version(novaVersao);
     }
   }
   ```
4. Flyway: aplique `db/archbase-offline-sync/V001__processed_sync_operation.sql`
   (renumere para a sequência do app). Se você copiou o script antes da 3.5.0, falta a coluna `user_id`, que a
   entidade mapeia desde a 3.0.10: o INSERT de cada operação falha e, com `ddl-auto=validate`, a aplicação não
   sobe. Acrescente uma migração com `ALTER TABLE processed_sync_operation ADD COLUMN user_id VARCHAR(64);`.
5. A limpeza remove operações com mais de 90 dias, todo dia às 03:00 — precisa de `@EnableScheduling` na aplicação.

### Só o módulo, sem o starter

Nada é registrado: a aplicação varre `br.com.archbase.offline.sync.service` e `br.com.archbase.offline.sync.web`
(ou declara os beans), inclui `br.com.archbase.offline.sync.persistence` nas entidades e repositórios, e segue os
passos 2 a 5. Sem varrer o pacote `web`, escreva o controller:

```java
@PostMapping("/api/v1/sync/operations")
SyncBatchResponseDTO sync(@RequestBody SyncBatchRequestDTO req) {
  return processor.process(req);
}
```

## Por que assim
Corrige os erros das tentativas anteriores no promotor: (a) lote em transação
única que revertia tudo ao primeiro conflito → aqui é `REQUIRES_NEW` por operação;
(b) idempotência só em cache TTL → aqui é durável; (c) conflito silencioso/só numa
entidade → aqui é tipado e uniforme; (d) contadores incompletos → `counts` cobre
todos os estados e `isSuccess()` nunca é `true` com operação pendente.
