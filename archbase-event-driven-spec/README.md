# archbase-event-driven-spec

Contratos de CQRS e mensageria do Archbase: interfaces de comando, consulta, evento, barramento, fábrica de
handlers, middleware e outbox, mais algumas anotações. Não tem implementação nem dependências próprias (o `pom.xml`
não declara nenhuma). A implementação está em [`archbase-event-driven`](../archbase-event-driven/README.md).

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-event-driven-spec</artifactId>
    <version>3.5.0</version>
</dependency>
```

## Auto-configuração

Não tem. É só biblioteca de interfaces e anotações.

## Contratos

Pacote base `br.com.archbase.event.driven.spec`.

| Pacote | Tipo | Assinatura |
|---|---|---|
| `message.contracts` | `Message<R>` | Marcador; `R` é o tipo do resultado |
| | `MessageBus` | `<R> R dispatch(Message<R>)` |
| | `MessageHandler<T extends Message<R>, R>` | `R handle(T)` |
| | `MessageHandlerFactory` | `<R> MessageHandler<Message<R>, R> createHandler(String messageName)` |
| `command.contracts` | `Command<R> extends Message<R>` | Marcador |
| | `CommandBus` | `<R> R dispatch(Command<R>)` |
| | `CommandHandler<C extends Command<R>, R>` | `R handle(C)` |
| | `CommandHandlerFactory` | `createCommandHandler(String commandName)` |
| `query.contracts` | `Query<R>`, `QueryBus`, `QueryHandler<Q, R>`, `QueryHandlerFactory` | Iguais aos de comando (`createQueryHandler`) |
| `event.contracts` | `Event<R> extends Message<R>` | Marcador |
| | `EventPublisher` | `<R> R publish(Event<R>)` |
| | `EventSubscriber` | `subscribe(Subscriber)`, `unsubscribe(Subscriber)` |
| | `EventBus extends EventSubscriber, EventPublisher` | |
| | `Subscriber` | `<R> void onEvent(Event<R>)` |
| | `EventHandler<E, R>`, `EventHandlerFactory` | Ver limitações |
| `middleware.contracts` | `Middleware` | `<R> R handle(Message<R>, NextMiddlewareFunction<Message<R>, R> next)` |
| | `NextMiddlewareFunction<T, R>` | `R call(T)` |
| `spring.contracts` | `HandlerFactoryConfig` | `getCommandHandlerFactory()`, `getQueryhandlerFactory()` |
| | `MiddlewareConfig` | `getCommandMiddlewarePipeline()`, `getQueryMiddlewarePipeline()`, `getEventMiddlewarePipeline()` (default: lista vazia) |
| `outbox.contracts` | `Outboxable` | `getAggregateId()`, `getAggregateType()`, `getPayload()`, `getType()`, `getTimestamp()` (`Long`) |
| | `OutboxListener` | `void onExportedEvent(Outboxable)` |

## Anotações

Pacote `br.com.archbase.event.driven.spec.annotations`.

| Anotação | Alvo | Lida por |
|---|---|---|
| `@CommandMapping(Class<? extends Command>)` (repetível, `@CommandMappings`) | classe do handler | `AutoScanHandlerFactory` (`archbase-event-driven`) |
| `@QueryMapping(Class<? extends Query>)` (repetível, `@QueryMappings`) | classe do handler | `AutoScanHandlerFactory` |
| `@HandlerScan(basePackages)` | bean do contexto | `SpringAutoScanHandlerFactory` |
| `@CommandHandler(namespace, name)` | método | ninguém |
| `@QueryHandler`, `@EventHandler`, `@EventSubscribe` | método | ninguém |

Exemplo de comando e handler (o registro é pela anotação de classe, não pelo método):

```java
public record CriarPedido(String cliente) implements Command<String> {}

@CommandMapping(CriarPedido.class)
public class CriarPedidoHandler implements CommandHandler<CriarPedido, String> {
    @Override
    public String handle(CriarPedido comando) {
        return "pedido-criado";
    }
}
```

## Limitações

- `@CommandHandler`, `@QueryHandler`, `@EventHandler` e `@EventSubscribe` (anotações de método) não são lidas por
  nenhum código do framework. Anotar um método com elas não registra handler algum. O que registra é implementar
  `CommandHandler`/`QueryHandler` e anotar a classe com `@CommandMapping`/`@QueryMapping` (ver
  [`archbase-event-driven`](../archbase-event-driven/README.md)).
- `EventHandler` e `EventHandlerFactory` não têm implementação nem uso. `EventHandlerFactory` declara um método
  chamado `createCommandHandler`.
- `EventSubscriber.subscribe/unsubscribe` e `EventPublisher.publish` devolvem um genérico `R`; a implementação
  existente devolve sempre `null`.

## Referência

- [`archbase-event-driven`](../archbase-event-driven/README.md): barramentos, varredura de handlers, middlewares e outbox.
