# archbase-offline-sync

Backend base do protocolo **batch offline-first** para apps Flutter que usam o
módulo `archbase_offline` do `archbase_flutter`. Ver o contrato em
`archbase-flutter/docs/design/archbase-offline-module.md` e a decisão em
`archbase-flutter/docs/adr/ADR-0001-motor-offline-first.md`.

## O que entrega
- **Endpoint** (o app expõe um controller fino):
  `POST /api/v1/sync/operations` → `SyncOperationProcessor.process(request)`.
- **Processamento por operação** com **transação própria** (`REQUIRES_NEW`):
  uma falha/conflito reverte só aquela operação, nunca o lote — o ACK é individual.
- **Idempotência durável** (`processed_sync_operation`): reenvio não reexecuta (→ SKIPPED).
- **Conflito tipado** (`SyncConflictException` → CONFLICT), **skip** (SKIPPED),
  **tipo desconhecido** (REJECTED), **dependências** no lote (BLOCKED) e
  **transitório** (FAILED, o cliente reenvia).

## Como o app usa
1. Adicione a dependência `br.com.archbase:archbase-offline-sync`.
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
4. Controller fino:
   ```java
   @PostMapping("/api/v1/sync/operations")
   SyncBatchResponseDTO sync(@RequestBody SyncBatchRequestDTO req) {
     return processor.process(req);
   }
   ```
5. Flyway: aplique `db/archbase-offline-sync/V001__processed_sync_operation.sql`
   (renumere para a sequência do app).
6. Retenção 90d: agende `ProcessedSyncOperationRepository.deleteOlderThan(cutoff)`.

## Por que assim
Corrige os erros das tentativas anteriores no promotor: (a) lote em transação
única que revertia tudo ao primeiro conflito → aqui é `REQUIRES_NEW` por operação;
(b) idempotência só em cache TTL → aqui é durável; (c) conflito silencioso/só numa
entidade → aqui é tipado e uniforme; (d) contadores incompletos → `counts` cobre
todos os estados e `isSuccess()` nunca é `true` com operação pendente.
