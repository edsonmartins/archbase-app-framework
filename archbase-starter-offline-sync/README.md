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

`archbase-offline-sync`, `spring-data-jpa`, `spring-boot-persistence` e `spring-boot-autoconfigure`.

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

Registradas, sem condição: `ArchbaseOfflineSyncPackagesAutoConfiguration` (antes das auto-configurações do JPA) e
`ArchbaseOfflineSyncAutoConfiguration` (depois delas).

| Mecanismo | O que registra |
|---|---|
| `@ComponentScan` dos pacotes `...offline.sync.service` e `...offline.sync.web` | `SyncOperationProcessor`, `SyncOperationExecutor`, `SyncStatusQueryService`, `ProcessedSyncOperationCleanup` e `ArchbaseSyncController` |
| Pacote `...offline.sync.persistence` somado aos pacotes de auto-configuração (e ao `@EntityScan` da aplicação, se houver) | A entidade `ProcessedSyncOperation` e, pela varredura do Boot, o `ProcessedSyncOperationRepository` |
| `@EnableJpaRepositories` só se o repositório ainda não existe | `ProcessedSyncOperationRepository`, quando a aplicação declarou o próprio `@EnableJpaRepositories` e o Boot não varreu |
| `@Bean` com `@ConditionalOnMissingBean(AppVersionGate.class)` | `archbaseDefaultAppVersionGate`, que aceita qualquer versão |

`ArchbaseSyncController` expõe:

- `POST /api/v1/sync/operations` — lê o cabeçalho `X-App-Version` e passa pelo `AppVersionGate` antes de
  processar o lote;
- `POST /api/v1/sync/operations/status` — reconciliação: diz quais `operationId` já foram processados, sem
  reexecutar nada.

`ProcessedSyncOperationCleanup` remove, todo dia às 03:00, registros de idempotência com mais de 90 dias
(retenção fixa no código). Sem `@EnableScheduling` na aplicação, não roda.

## Cuidados

- **Não escreva o controller**: com este starter a rota já existe, e um segundo mapeamento idêntico impede a subida.
- A entidade e o repositório do módulo somam aos da aplicação. Até a 3.5.0 o starter declarava `@EntityScan` e
  `@EnableJpaRepositories` próprios, e adicioná-lo fazia sumir os repositórios da aplicação.
- As rotas `/api/v1/sync/**` ficam sob a segurança da aplicação como qualquer outra; o starter não as libera nem
  as protege.

## Referência

- Módulo e protocolo: [archbase-offline-sync](../archbase-offline-sync/README.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
