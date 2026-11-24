# archbase-domain-driven-design

Implementação JPA dos contratos do [`archbase-domain-driven-design-spec`](../archbase-domain-driven-design-spec/README.md):
a classe base de repositório (`CommonArchbaseJpaRepository`) com QueryDSL, RSQL e Envers; serviços e controllers
REST genéricos; um repositório JDBC com QueryDSL SQL; o módulo Jackson de identificadores tipados; e um
publicador de eventos simples.

Não tem auto-configuração. Quem põe o repositório base em uso é o
[`archbase-starter-core`](../archbase-starter-core/README.md); o resto a aplicação registra ou estende.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-domain-driven-design</artifactId>
    <version>3.5.0</version>
</dependency>
```

Normalmente vem pelo `archbase-starter-core`.

## O que traz

| Dependência | Para quê |
|---|---|
| `archbase-domain-driven-design-spec` | Contratos, bases de entidade |
| `archbase-query` | RSQL (`ArchbaseRSQLJPASupport`), usado em `findAll(String, Pageable)` |
| `archbase-error-handling` | Exceções e mensagens |
| `querydsl-jpa`, `querydsl-sql`, `querydsl-sql-codegen`, `querydsl-apt` (OpenFeign) | Consultas tipadas |
| `hibernate-envers`, `spring-data-envers` | `RevisionRepository` |
| `spring-boot-starter-data-jdbc` | Repositório JDBC. Em escopo `compile`: traz a auto-configuração do Spring Data JDBC para toda aplicação que usa este módulo |
| `spring-web`, `tika-core`, `guava`, `log4j-api`, `jakarta.validation-api`, `tools.jackson.core:jackson-databind` | Controllers, detecção de tipo MIME, utilitários |

## Repositório JPA

| Tipo | Papel |
|---|---|
| `ArchbaseCommonJpaRepository<T, ID, N>` | Interface para entidades JPA comuns (`PersistenceEntityBase` etc.) |
| `ArchbaseJpaRepository<T, ID, N>` | Mesma API, para agregados: `T extends AggregateRoot<T, ID>`, `ID extends Identifier` |
| `CommonArchbaseJpaRepository<T, ID, N>` | Implementação (estende `SimpleJpaRepository`) |
| `SimpleArchbaseJpaRepository<T, ID, N>` | Implementação para agregados; nenhum starter a usa |

`N` é o tipo do número de revisão do Envers.

O `archbase-starter-core` registra os repositórios dos pacotes de `archbase.app.jpa.repositories` com
`repositoryBaseClass = CommonArchbaseJpaRepository`. Sem o starter, declare:

```java
@EnableJpaRepositories(basePackages = "com.minhaempresa.repositorio",
                       repositoryBaseClass = CommonArchbaseJpaRepository.class)
```

```java
public interface PedidoRepository extends ArchbaseCommonJpaRepository<Pedido, String, Long> {
}

// QueryDSL
QPedido p = QPedido.pedido;
List<Pedido> abertos = repo.query(q -> q.select(p).from(p).where(p.status.eq("ABERTO")).fetch());
long removidos = repo.deleteWhere(p.status.eq("CANCELADO"));

// Specification do Spring Data (JpaSpecificationExecutor) com RSQL
Page<Pedido> page = repo.findAll(ArchbaseRSQLJPASupport.<Pedido>toSpecification("status==ABERTO"), pageable);
```

Métodos além do Spring Data: `query(Function<JPAQuery<?>, O>)`, `update(Consumer<JPAUpdateClause>)`,
`deleteWhere(Predicate)`, `jpaSqlQuery`, `jpaSqlSubQuery`, `executeStoredProcedure(nome, builder)`, `findOne`/
`findAll` com `JPQLQuery` ou `FactoryExpression` (projeções), e os herdados de `Repository`: `save(T...)`,
`findAll(Predicate...)`, `matching`, `howMany`, `containsAny`, `findAll(String filter, Pageable)`.

As classes Q são geradas pela aplicação (APT do QueryDSL; ver [`archbase-annotation-processor`](../archbase-annotation-processor/README.md)).
O `jpaSqlQuery` escolhe os templates SQL pelo `DatabaseMetaData` da conexão, na criação do repositório.

### `findAll(String filter, Pageable)`

O texto é lido primeiro como JSON `{"search": "...", "fields": "campo1,campo2"}`: busca o valor de `search` em cada
campo listado, combinados com OU (`SimpleFilterPredicateFactory`). Se não houver campo aplicável ao valor, lança
`IllegalArgumentException`. Se o texto não for JSON, é tratado como RSQL (`ArchbaseRSQLJPASupport.rsql`).

## Serviços e controllers genéricos

Classes abstratas; a aplicação estende e implementa `getRepository()` (e `getService()` nos controllers).

| Serviço | Controller | Endpoints |
|---|---|---|
| `CommonArchbaseQueryService`, `SimpleArchbaseQueryService` | `CommonArchbaseQueryRestController`, `SimpleArchbaseQueryRestController` | `GET /{id}`, `POST /findByComplexId`, `GET /findAll?page&size[&sort]`, `GET /findAll?ids`, `GET /findWithFilter?filter&page&size`, `GET /findWithFilterAndSort`, `GET /count`, `GET /exists/{id}`, `POST /existsByComplexId` |
| `CommonArchbaseService`, `SimpleArchbaseService`, `CommonArchbaseCommandService`, `SimpleArchbaseCommandService` | `CommonArchbaseRestController`, `SimpleArchbaseRestController`, `CommonArchbaseCommandRestController`, `SimpleArchbaseCommandRestController` | os de consulta + `POST/PUT /`, `POST/PUT /validate`, `POST/PUT /validateGroup`, `DELETE /` (id no corpo) |

Cuidados:

- **`save`, `validate` e `validateGroup` não validam.** Chamam `validator.validate(objeto)` e descartam o resultado:
  objeto inválido é gravado sem erro. Valide antes, ou sobrescreva o método.
- Os controllers não têm `@HasPermission`: ficam protegidos só pela cadeia de filtros (autenticado). Para exigir
  permissão, sobrescreva os métodos na sua classe e anote-os.
- `GET /{id}` converte o texto do caminho para `ID` com um cast sem verificação: só funciona com `ID = String`. Sem
  resultado, devolve `200` com corpo vazio.
- `removeById` sem registro lança `ArchbaseServiceException`.

## Especificações traduzidas para JPA

`br.com.archbase.ddd.domain.specification.basic` tem especificações por valor de campo (`EqualityArchbaseSpecification`,
`LikeArchbaseSpecification`, `BetweenArchbaseSpecification`, `In`, `NotIn`, `GreaterThan`, `LessThan` etc.) e
`infraestructure.persistence.jpa.specification` os conversores para `Predicate` (`SpecificationTranslator`,
`SpecificationTranslatorFactoryImpl`).

> **Na 3.5.0 `matching`, `howMany` e `containsAny` não funcionam.** O repositório usa um `SpecificationTranslator`
> que só é atribuído por `setTranslator(...)`, e nada no framework o chama: as três chamadas lançam
> `NullPointerException`. Use `Specifications.findAllSatisfying` em memória ou RSQL / `Specification` do JPA.

## Identificadores tipados no JSON

`ArchbaseIdentifierJacksonModule` (Jackson 3) serializa `record PedidoId(UUID value) implements Identifier` como o
valor (`"3f1c…"`) em vez de objeto. Não é registrado automaticamente, porque muda o JSON; exponha-o como bean:

```java
@Bean
JacksonModule archbaseIdentifierModule() {
    return new ArchbaseIdentifierJacksonModule();
}
```

Não cobre identificadores usados como chave de `Map`. Os conversores JPA e Spring estão no
[`archbase-domain-driven-design-spec`](../archbase-domain-driven-design-spec/README.md).

## Repositório JDBC

`ArchbaseJdbcRepository<T, ID>` (Spring Data JDBC + QueryDSL SQL: `findOne(Predicate)`, `findAll(Predicate)`,
`query`, `update`, `deleteWhere`). Use com
`@EnableJdbcRepositories(repositoryFactoryBeanClass = ArchbaseJdbcRepositoryFactoryBean.class)`; o factory bean
precisa de um `SQLQueryFactory` no contexto. O pacote `persistence.jdbc.apt` traz um processador QueryDSL para
classes com `org.springframework.data.annotation.Id` (`SpringDataJdbcAnnotationProcessor`); ele não está registrado em
`META-INF/services`, então só roda se configurado explicitamente no compilador.

## Eventos

`SimpleEventPublisher` (bean `eventPublisher`) e `EventListenerBeanPostProcessor` registram métodos anotados com
`@DomainEventListener` e os chamam em `publish(Event)`. Nenhum starter varre o pacote
`br.com.archbase.ddd.infraestructure.events`; inclua-o na varredura (`archbase.app.component.scan`) para usar.

- Exceção num handler é registrada em log e engolida.
- `@DomainEventListener(asynchronous = true)` cria um `AsynchronousEventHandler`, que não sobrescreve nada: o
  handler roda **na mesma thread**, como o síncrono.
- Para CQRS (`CommandBus`, `EventBus`, `QueryBus`), use o `archbase-event-driven`.

## Limitações

- **`@StorageField` não tem efeito.** O `ArchbaseStorageFieldAspect`, que enviaria campos anotados para o
  `ArchbaseStoragePort`, está com `@Aspect` e `@Component` comentados e não é registrado. A propriedade
  `archbase.storage.data-url.max-bytes` (padrão `20971520`) que ele lê fica, portanto, sem efeito.
- Os métodos de `RevisionRepository` exigem a entidade auditada pelo Envers (`@Audited`).
- O módulo tem testes só para o aspecto de armazenamento (métodos isolados) e o módulo Jackson.

## Referência

- Contratos e bases de entidade: [archbase-domain-driven-design-spec](../archbase-domain-driven-design-spec/README.md)
- RSQL: [archbase-query](../archbase-query/README.md)
- Registro dos repositórios e varredura: [archbase-starter-core](../archbase-starter-core/README.md)
- Métodos `persist`/`update` sem SELECT: [archbase-hypersistence](../archbase-hypersistence/README.md)
- Visão geral do framework: [CLAUDE.md](../CLAUDE.md)
