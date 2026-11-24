# archbase-starter-offline-sync

Auto-configuração do [archbase-offline-sync](../archbase-offline-sync/README.md), o backend do protocolo batch
offline-first. O protocolo, os handlers e os estados de cada operação estão no README do módulo; aqui fica só o que
o starter registra e o que a aplicação ainda precisa fazer.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-offline-sync</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

`archbase-offline-sync`, `spring-data-jpa`, `spring-boot-persistence` (`@EntityScan`) e `spring-boot-autoconfigure`.

## O que é preciso ligar

Não há propriedade: o starter carrega sempre que está no classpath. A aplicação precisa:

| O quê | Por quê |
|---|---|
| Bean `SyncTenantProvider` | Obrigatório: `SyncOperationExecutor` e `SyncStatusQueryService` o recebem no construtor; sem ele, não sobe |
| Um bean `SyncOperationHandler` por tipo de operação | Tipo sem handler é `REJECTED` |
| Bean `SyncUserProvider` | Opcional; preenche o usuário no registro de idempotência |
| Bean `AppVersionGate` | Opcional; o padrão aceita qualquer versão. Para forçar atualização, lance `SyncAppVersionIncompatibleException` (426) |
| Tabela `processed_sync_operation` | Aplique `db/archbase-offline-sync/V001__processed_sync_operation.sql` nas suas migrações; fica fora das locations do Flyway |
| `@EnableScheduling` | Só para a limpeza diária rodar |

## Auto-configuração

Registrada: `ArchbaseOfflineSyncAutoConfiguration`, sem condição.

| Mecanismo | O que registra |
|---|---|
| `@ComponentScan` dos pacotes `...offline.sync.service` e `...offline.sync.web` | `SyncOperationProcessor`, `SyncOperationExecutor`, `SyncStatusQueryService`, `ProcessedSyncOperationCleanup` e `ArchbaseSyncController` |
| `@EntityScan` | A entidade `ProcessedSyncOperation` |
| `@EnableJpaRepositories` | `ProcessedSyncOperationRepository` |
| `@Bean` com `@ConditionalOnMissingBean(AppVersionGate.class)` | `archbaseDefaultAppVersionGate`, que aceita qualquer versão |

`ArchbaseSyncController` expõe:

- `POST /api/v1/sync/operations` — lê o cabeçalho `X-App-Version` e passa pelo `AppVersionGate` antes de
  processar o lote;
- `POST /api/v1/sync/operations/status` — reconciliação: diz quais `operationId` já foram processados, sem
  reexecutar nada.

`ProcessedSyncOperationCleanup` remove, todo dia às 03:00, registros de idempotência com mais de 90 dias
(retenção fixa no código). Sem `@EnableScheduling` na aplicação, não roda.

## Cuidados

- **Não escreva o controller.** O passo 4 do [README do módulo](../archbase-offline-sync/README.md) manda a
  aplicação expor `POST /api/v1/sync/operations`; com este starter, essa rota já existe, e um segundo mapeamento
  idêntico impede a subida (mapeamento ambíguo).
- O starter declara `@EntityScan` e `@EnableJpaRepositories` próprios. Quando há `@EntityScan` declarado, o Spring
  Boot deixa de usar o pacote da classe `@SpringBootApplication` como padrão para entidades; declare os pacotes da
  aplicação (`@EntityScan`/`@EnableJpaRepositories` ou `archbase.app.jpa.entities`/`archbase.app.jpa.repositories`
  do `archbase-starter-core`).
- As rotas `/api/v1/sync/**` ficam sob a segurança da aplicação como qualquer outra; o starter não as libera nem
  as protege.

## Referência

- Módulo e protocolo: [archbase-offline-sync](../archbase-offline-sync/README.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
