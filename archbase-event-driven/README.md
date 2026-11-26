# archbase-event-driven

Implementação em memória e síncrona dos contratos de [`archbase-event-driven-spec`](../archbase-event-driven-spec/README.md):
barramentos de comando, consulta e evento, descoberta de handlers por varredura de pacote, dois middlewares
(log e métricas) e uma entidade de outbox. Não há broker, fila nem processamento assíncrono.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-event-driven</artifactId>
    <version>3.5.0</version>
</dependency>
```

Traz `archbase-event-driven-spec`, `archbase-domain-driven-design-spec` (e com ele Spring Context, Spring Data
Commons e Hibernate), `jakarta.validation-api` e `org.reflections:reflections`.

## Auto-configuração

Não tem: o módulo não tem `AutoConfiguration.imports`, e nenhum starter do Archbase depende dele ou varre seus
pacotes (o `archbase-starter-messaging` traz só o `-spec`). Para ter os barramentos como beans, a aplicação importa a
configuração:

```java
@Configuration
@Import(MessageBusSpringConfiguration.class)
@HandlerScan(basePackages = "com.acme.pedidos.handlers")
public class BarramentoConfig {
}
```

`MessageBusSpringConfiguration` (`br.com.archbase.event.driven.bus.spring`) registra:

| Bean | Tipo | Como é montado |
|---|---|---|
| `getCommandBus` | `CommandBus` → `SimpleCommandBus` | fábrica de handlers + `getCommandMiddlewarePipeline()` |
| `getQueryBus` | `QueryBus` → `SimpleQueryBus` | fábrica de handlers + `getQueryMiddlewarePipeline()` |

Não registra `EventBus`. Não há propriedades `archbase.*`.

### Personalização

- **Fábrica de handlers:** se algum bean anotado com `@Configuration` implementar `HandlerFactoryConfig`, ele é
  usado; senão, `DefaultHandlerFactoryConfig`, que cria um `SpringAutoScanHandlerFactory`.
- **Middlewares:** se algum bean `@Configuration` implementar `MiddlewareConfig`, ele é usado; senão,
  `DefaultMiddlewareConfig`, cujas três listas são `[MetricsMiddleware, LoggingMiddleware]`.

## Descoberta de handlers

`SpringAutoScanHandlerFactory` junta os `basePackages` de todos os beans anotados com `@HandlerScan` e, para cada
pacote, `AutoScanHandlerFactory.scanAndRegisterHandlers` usa Reflections para achar implementações de
`CommandHandler` e `QueryHandler`:

- classe com `@CommandMapping(X.class)` / `@QueryMapping(X.class)` (ou as versões repetidas): registrada para o nome
  de `X`;
- handler de comando com método público cujo primeiro parâmetro é um tipo anotado com `@DomainCommand`
  (`archbase-domain-driven-design-spec`): registrado para esse tipo.

O despacho procura o handler pelo nome exato da classe da mensagem (`message.getClass().getName()`); subclasse não
casa. Sem handler, `NoHandlerFoundException` (extends `CommandBusException`, `RuntimeException`).

A cada despacho o handler é **instanciado de novo** por `AutowireCapableBeanFactory.createBean(classe)`: tem
injeção de dependências, mas não é o singleton do contexto, e não precisa (nem deve) ser `@Component`.

```java
public record BuscarPedido(String id) implements Query<PedidoDto> {}

@QueryMapping(BuscarPedido.class)
public class BuscarPedidoHandler implements QueryHandler<BuscarPedido, PedidoDto> {
    private final PedidoRepository repositorio;

    public BuscarPedidoHandler(PedidoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public PedidoDto handle(BuscarPedido consulta) {
        return repositorio.buscar(consulta.id());
    }
}

// uso
PedidoDto dto = queryBus.dispatch(new BuscarPedido("42"));
```

Sem Spring, `new AutoScanHandlerFactory(beanFactory)` + `scanAndRegisterHandlers("pacote")` e
`new SimpleCommandBus(fabrica, middlewares)` / `new SimpleQueryBus(fabrica, middlewares)` fazem o mesmo.

## Middlewares

`SimpleMessageBus` encadeia os middlewares na ordem da lista e chama o handler no fim, na thread de quem despachou.

| Classe | Faz |
|---|---|
| `LoggingMiddleware` | Loga em INFO a mensagem recebida e o resultado (`toString()` de ambos); em ERROR a falha, e relança |
| `MetricsMiddleware` | Com o Micrometer no classpath, registra `archbase.<tipo>.success`, `.dispatched`, `.error` (tag `exception`) e o timer `.duration` (tag `status`), com as tags `type` e `message`. O construtor padrão usa o `Metrics.globalRegistry`, onde o Spring Boot registra o seu; `new MetricsMiddleware(registry)` usa o informado. Sem Micrometer, só repassa |

## Eventos

`SimpleEventBus` (implementa `EventBus`) guarda `Subscriber`s num `HashSet` e, em `publish`, chama `onEvent` de
cada um na mesma thread, sem ordem definida e sem middleware. Não é bean: crie e compartilhe a instância.
`SimpleEventBusPublisher` e `SimpleEventBusSubscriber` só delegam para um `EventBus`.

## Outbox

- `OutboxEvent`: entidade JPA (`id` UUID gravado como texto e gerado, `aggregateType`, `aggregateId`, `type`,
  `timestamp` `Long`, `payload` até 1 MB), com `OutboxEvent.from(Outboxable)`.
- `OutboxEventRepository`: `CrudRepository<OutboxEvent, UUID>`.
- `OutboxEventListener`: implementa `OutboxListener` com corpo vazio.

Nada no módulo grava, lê ou retransmite eventos de outbox. Para usar a entidade e o repositório, a aplicação inclui
`br.com.archbase.event.driven.outbox` no scan de entidades e de repositórios JPA e escreve o próprio relay.

## Limitações

- Até a 3.5.0 o `CommandBus` recebia o pipeline de consultas (um `getCommandMiddlewarePipeline()` próprio não
  tinha efeito) e o `MetricsMiddleware` não registrava nenhuma métrica: chamava o Micrometer por reflexão com nomes
  errados e engolia os erros.
- **`LoggingMiddleware` loga conteúdo em INFO:** o `toString()` de toda mensagem e resultado. Cuidado com dados
  sensíveis; troque a lista com um `MiddlewareConfig` próprio.
- As anotações de método `@CommandHandler`, `@QueryHandler`, `@EventHandler` e `@EventSubscribe` não são lidas.
- Não há barramento assíncrono, retry nem transação em torno do despacho.

## Referência

- [`archbase-event-driven-spec`](../archbase-event-driven-spec/README.md): os contratos.
- [`archbase-modular-monolith`](../archbase-modular-monolith/README.md): eventos de integração entre módulos (outro barramento).
