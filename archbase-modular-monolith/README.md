# archbase-modular-monolith

Apoio a monólito modular: declaração de módulos com `@Module`, registro e ordem de inicialização por dependência,
ciclo de vida, eventos de integração em memória, chamadas síncronas entre módulos por um gateway e regras ArchUnit
de fronteira entre módulos.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-modular-monolith</artifactId>
    <version>3.5.0</version>
</dependency>
```

Traz `archbase-domain-driven-design-spec`, `archbase-event-driven`, `archbase-architecture-rules` (escopo
compile), `spring-boot-starter`, `spring-boot-starter-actuator`, `spring-boot-health`,
`spring-boot-starter-data-jpa`, `tools.jackson.core:jackson-databind` e `com.tngtech.archunit:archunit` 1.4.2 (a versão do pom raiz)
(ver limitações).

## Auto-configuração

`ModuleAutoConfiguration`, registrada em `AutoConfiguration.imports`. Liga por padrão: basta o jar no classpath.

| Propriedade | Padrão | Efeito |
|---|---|---|
| `archbase.modulith.enabled` | `true` | `false` desliga a auto-configuração inteira |
| `archbase.modulith.validate-dependencies-on-startup` | `true` | Valida dependências ausentes e ciclos antes de iniciar os módulos |
| `archbase.modulith.event-bus.thread-pool-size` | `10` | Threads do `IntegrationEventBus` |
| `archbase.modulith.gateway.thread-pool-size` | `10` | Threads do `ModuleGateway` |
| `archbase.modulith.health-checks-enabled` | `true` | Registra o `ModuleHealthIndicator` (com o actuator no classpath) |
| `archbase.modulith.scan-packages` | vazio | Sem efeito: não é lida |
| `archbase.modulith.metrics-enabled` | `true` | Sem efeito: não há métricas |
| `archbase.modulith.event-bus.use-outbox` | `true` | Sem efeito: não é lida |
| `archbase.modulith.gateway.default-timeout-seconds` | `30` | Sem efeito: o timeout padrão é fixo em 30 s no código |

Beans registrados:

| Bean | Condição | Tipo |
|---|---|---|
| `moduleRegistry` | `@ConditionalOnMissingBean` | `DefaultModuleRegistry` |
| `integrationEventBus` | `@ConditionalOnMissingBean` | `SimpleIntegrationEventBus("main", pool fixo, ObjectMapper)` |
| `moduleGateway` | `@ConditionalOnMissingBean` | `SimpleModuleGateway(registry, pool fixo)` |
| `modulithObjectMapper` | `@ConditionalOnMissingBean` (`tools.jackson.databind.ObjectMapper`) | `JsonMapper` com `findAndAddModules` |
| `moduleDiscoveryListener` | sempre | `ModuleAutoConfiguration.ModuleDiscoveryListener` |

`ModuleHealthIndicator` é registrado pela auto-configuração quando o actuator está no classpath: a saúde de cada
módulo aparece em `/actuator/health`. Módulo desligado (`enabled = false`) conta como saudável; módulo que falhou ao
subir deixa o indicador `DOWN`. Até a 3.5.0 era `@Component` e só aparecia se a aplicação varresse o pacote.

`@EnableModularMonolith` só faz `@Import(ModuleAutoConfiguration.class)`. Seus atributos (`basePackages`,
`validateDependenciesOnStartup`, `enableHealthChecks`, `enableMetrics`, `defaultCommunicationMode`) não são lidos;
use as propriedades acima.

## Módulos

`@Module` (`br.com.archbase.modulith.annotations`) é meta-anotada com `@Component`: a classe precisa estar no
component scan da aplicação. Atributos: `name` (obrigatório), `version` (`"1.0.0"`), `dependsOn` (nomes de
módulos), `description`, `enabled` (`true`), `order` (`0`).

No `ContextRefreshedEvent` do próprio contexto, `ModuleDiscoveryListener`:

1. registra no `ModuleRegistry` um `ModuleDescriptor` por bean com `@Module` (nome repetido lança
   `ModuleRegistrationException`);
2. com `validate-dependencies-on-startup`, chama `validateDependencies()`, que lança `ModuleDependencyException`
   para dependência não registrada ou ciclo;
3. inicia os módulos habilitados em ordem topológica (empate: `order`, depois nome): estado `STARTING`, chama
   `onStart(ModuleContext)` se o bean implementar `ModuleLifecycle`, estado `STARTED`. Exceção: estado `FAILED`,
   `onError`, e `ModuleException` relançada.

```java
@Module(name = "pedidos", dependsOn = "clientes")
public class PedidosModule implements ModuleLifecycle {

    @Override
    public void onStart(ModuleContext context) {
        context.getIntegrationEventBus().subscribe(ClienteBloqueado.class, this::suspenderPedidos);
        ((SimpleModuleGateway) context.getModuleGateway())
                .registerHandler("pedidos", TotalDoCliente.class, req -> calcularTotal(req.clienteId()));
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}
```

`ModuleContext` dá acesso ao descritor, ao registry, ao gateway, ao barramento, a `getBean(...)` do
`ApplicationContext`, a atributos (`setAttribute`/`getAttribute`) e a `isModuleAvailable(nome)`.

## Eventos de integração

Eventos implementam `IntegrationEvent` (`communication.contracts`, estende `Serializable`): obrigatórios
`getAggregateId()` e `getAggregateType()`; `getEventType()` (padrão: nome simples da classe), `getOccurredAt()`,
`getEventId()`, `getSourceModule()` e `getVersion()` têm default.

```java
public record ClienteBloqueado(String clienteId) implements IntegrationEvent {
    public String getAggregateId() { return clienteId; }
    public String getAggregateType() { return "cliente"; }
}

integrationEventBus.publish(new ClienteBloqueado("42"));
```

`SimpleIntegrationEventBus`:

- `publish`: entrega a cada assinante do tipo **exato** do evento, cada um numa tarefa do pool. Exceção do assinante
  é logada e descartada; quem publica não fica sabendo.
- `publishReliably(evento, entityManager)`: faz `persist` de um `OutboxEventRecord` (tabela `outbox_event`) com o
  JSON do evento e em seguida chama `publish`, sem esperar o commit.
- `subscribe(tipo, handler)`, `subscribe(tipo, sourceModule, handler)`, `unsubscribe`, `unsubscribeAll`.

## Gateway entre módulos

`ModuleGateway.execute(modulo, request)` (timeout de 30 s), `execute(modulo, request, Duration)`,
`executeAsync(...)`, `isModuleAvailable(nome)` (registrado, `STARTED` e habilitado) e `waitForModule(nome, Duration)`.
O pedido implementa `ModuleRequest<R>`; o handler é `ModuleRequestHandler<T, R>`, procurado pela classe exata do
pedido. Erros: `ModuleNotFoundException` (módulo indisponível), `ModuleRequestException` (sem handler ou falha
verificada), `ModuleRequestTimeoutException`; `RuntimeException` do handler é relançada como está.

`registerHandler` e `unregisterHandler` existem só em `SimpleModuleGateway`, não na interface.

## Regras ArchUnit

Pacote `br.com.archbase.modulith.rules`, métodos estáticos que devolvem `ArchRule`:

- `ModuleBoundaryRules`: `modulesShouldNotHaveCyclicDependencies(padraoDeSlice)`,
  `internalApisShouldResideInInternalPackages()`, `internalPackageClassesShouldNotBePublic()`,
  `moduleClassesShouldBeConfigurations()`, `moduleApisShouldBeInterfaces()`, `integrationEventsShouldBeImmutable()`,
  `modulesShouldNotAccessOtherModulesInternals(pacoteBase)` e `checkAll(classes, padrao)` (ciclos, `@InternalApi`,
  `@ModuleApi`, imutabilidade).
- `ModuleDependencyRules`: `moduleShouldNotDependOn`, `moduleShouldOnlyDependOn`,
  `modulesShouldHaveAcyclicDependencies` e o builder `forModules(base).forbid(a, b).allow(a, b...).check(classes)`.
- `IntegrationEventRules`: implementar `IntegrationEvent`, ser serializável, ser imutável, nome `*Event`, pacotes
  permitidos.

```java
JavaClasses classes = new ClassFileImporter().importPackages("com.acme.app");
ModuleBoundaryRules.modulesShouldNotHaveCyclicDependencies("com.acme.app.modules.(*)..").check(classes);
```

## Limitações

- Até a 3.5.0 o módulo declarava ArchUnit 1.0.1, que não lê bytecode Java 25: as regras falhavam com "failed to
  check any classes" ou, com `allowEmptyShould`, passavam sem examinar nada. Com 1.4.2 elas examinam as classes, e
  quem as usa pode ver violações que antes não apareciam.
- `subscribe(tipo, sourceModule, handler)` filtra pela origem do evento: `getSourceModule()` ou, se vier vazio, o
  `sourceModule` da anotação `@IntegrationEvent`. Evento sem origem só chega a quem assinou sem `sourceModule`.
- No fechamento do contexto, `ModuleLifecycle.onStop()` é chamado em cada módulo iniciado, na ordem de
  `getShutdownOrder()`.
- Até a 3.5.0: o filtro comparava com o nome do barramento (`"main"`) e quem assinava por módulo não recebia nada;
  `onStop` nunca era chamado; e módulo desligado deixava o health `DOWN`.
- **Outbox sem relay e com tabela disputada:** nada lê `outbox_event`. `OutboxEventRecord` é entidade aninhada em
  `SimpleIntegrationEventBus` (precisa estar no scan de entidades) e usa a mesma tabela que o `OutboxEvent` de
  `archbase-event-driven` mapeia pela nomenclatura padrão, com `timestamp` `Instant` lá `Long` e `id` texto.
- `moduleClassesShouldBeConfigurations()` aceita a meta-anotação: uma classe só com `@Module` é componente e passa.
  `modulesShouldNotAccessOtherModulesInternals(base)` considera módulo o primeiro segmento depois de `base` e só acusa
  acesso ao `..internal..` de **outro** módulo. Até a 3.5.0 a primeira exigia `@Configuration`/`@Component` diretos e a
  segunda proibia também o `internal` do próprio módulo.
- `@ModuleDependency`/`@ModuleDependencies`, `@IntegrationEvent` (`reliable`, `retentionMinutes` etc.) e
  `@InternalApi.severity` não são lidos em tempo de execução; só `dependsOn` de `@Module` entra no grafo.

## Referência

- [`archbase-event-driven`](../archbase-event-driven/README.md): barramentos CQRS e o `OutboxEvent`.
- [`archbase-architecture-rules`](../archbase-architecture-rules/README.md): regras gerais de arquitetura e a nota
  sobre a versão do ArchUnit.
