# archbase-hypersistence

Empacota o [Hypersistence Utils](https://github.com/vladmihalcea/hypersistence-utils) na linha certa do Hibernate
que o Spring Boot 4.1 entrega, com um utilitário de contagem de SQL para testes e um repositório com
`persist`/`merge`/`update`. Os tipos JSON, array e range são do próprio Hypersistence Utils: este módulo não os
reimplementa nem os registra, só garante a dependência.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-hypersistence</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase. Quem quer a
auto-configuração usa o [archbase-starter-hypersistence](../archbase-starter-hypersistence/README.md), que depende
deste módulo.

## O que traz

| Dependência | Escopo | Observação |
|---|---|---|
| `io.hypersistence:hypersistence-utils-hibernate-73` | compile | Compilado contra Hibernate 7.3/7.4; o Boot 4.1 entrega 7.4 |
| `spring-boot-starter-data-jpa` | compile | |
| `archbase-domain-driven-design-spec` | compile | |
| `com.fasterxml.jackson.core:jackson-databind` (Jackson 2) | compile | É o que o `JsonType` do Hypersistence Utils usa internamente |
| `org.postgresql:postgresql` | **provided** | Não chega à aplicação: declare o driver você mesmo |

O `net.ttddyy:datasource-proxy`, de que o `SQLStatementCountAssertions` precisa, **não** vem (no Hypersistence Utils
ele é `provided`/opcional).

## Auto-configuração

Nenhuma. O módulo não tem `AutoConfiguration.imports`. A única classe de configuração,
`ArchbaseHypersistenceConfiguration`, só habilita `ArchbaseHypersistenceProperties` e só é carregada quando
importada — o starter faz isso. As propriedades `archbase.hypersistence.*` estão descritas no
[README do starter](../archbase-starter-hypersistence/README.md); fora `enabled`, nenhuma muda comportamento.

Os tipos do Hypersistence Utils não precisam de configuração Spring: são tipos Hibernate usados por anotação na
entidade, e funcionam com ou sem o starter.

## Uso

### Tipos JSON, array e range

Entidades do Archbase estendem `PersistenceEntityBase` ou `TenantPersistenceEntityBase` (não genéricas, id
`String`):

```java
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "PRODUTO")
public class Produto extends TenantPersistenceEntityBase {

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadados;

    @Type(StringArrayType.class)
    @Column(columnDefinition = "text[]")
    private String[] codigos;

    @Type(PostgreSQLRangeType.class)
    @Column(columnDefinition = "daterange")
    private Range<LocalDate> vigencia;
}
```

O teste do módulo (`TiposPostgreSQLFuncionamTest`, PostgreSQL 16 via Testcontainers) faz o roundtrip de
`Map`/`List` em `jsonb`, `String[]` em `text[]` e `Range<LocalDate>` em `daterange`. Um detalhe que ele registra:
o PostgreSQL normaliza `daterange` para `[início,fim)`, então `Range.closed(1/1, 31/12)` volta com limite
superior `1/1` do ano seguinte.

Compatibilidade: tipos JSON valem para PostgreSQL, MySQL, Oracle e H2; array, range, `PostgreSQLHStoreType` e
`PostgreSQLInetType` são só PostgreSQL.

### TSID

Use a anotação do próprio Hypersistence Utils, `io.hypersistence.utils.hibernate.id.Tsid`, no `@Id` (`Long` ou
`String`). `ArchbaseTsidGenerator` é só documentação: tem `@Target(ANNOTATION_TYPE)` e retenção `SOURCE`, não
pode ser posta em campo e não gera nada.

### Contagem de SQL em testes

`SQLStatementCountAssertions` delega ao `SQLStatementCountValidator` do Hypersistence Utils:

```java
SQLStatementCountAssertions.reset();
List<Pedido> pedidos = pedidoRepository.findAllComItens();
SQLStatementCountAssertions.assertSelectCount(1);
```

Métodos: `reset`, `assertSelectCount`, `assertInsertCount`, `assertUpdateCount`, `assertDeleteCount`,
`assertTotalCount`, `assertNoSelect`, `assertNoInsert`, `assertNoUpdate`, `assertNoDelete`.

Só conta o que passa por um `DataSource` envolvido pelo datasource-proxy com contagem de consultas
(`ProxyDataSourceBuilder ... .countQuery()`). Adicione `net.ttddyy:datasource-proxy` ao escopo de teste e
envolva o `DataSource`. O javadoc da classe sugere `spring.datasource.driver-class-name=...ProxyDataSourceBuilder`:
isso não funciona, `ProxyDataSourceBuilder` não é um driver JDBC.

## Limitações

**`ArchbaseHypersistenceRepository` não é conectado a nada.** A interface declara `persist`, `persistAndFlush`,
`persistAll`, `merge`, `mergeAll`, `update`, `updateAll` e as variantes `...AndFlush`; a implementação é
`ArchbaseHypersistenceRepositoryImpl`, que estende `SimpleJpaRepository` e só tem construtores
`(JpaEntityInformation, EntityManager)` e `(Class<T>, EntityManager)`. Nenhum código do framework a registra, e
nenhum teste exercita a interface num repositório. Em particular, a combinação
`extends ArchbaseCommonJpaRepository<T, String, Long>, ArchbaseHypersistenceRepository<T, String>` não tem
caminho de implementação: a classe não serve como fragmento do Spring Data (precisaria de construtor
injetável) e, usada como `repositoryBaseClass`, substitui a `CommonArchbaseJpaRepository`, que implementa os
métodos QueryDSL do `ArchbaseCommonJpaRepository`. Para inserir sem o `SELECT` do `save()`, chame
`EntityManager.persist` diretamente.

Mesmo como base de repositório, `update(entity)` é `entityManager.merge(entity)` — igual a `merge` —, então
não evita o `SELECT` de uma entidade destacada.

## Referência

- Starter com auto-configuração: [archbase-starter-hypersistence](../archbase-starter-hypersistence/README.md)
- Documentação dos tipos: [Hypersistence Utils](https://github.com/vladmihalcea/hypersistence-utils)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
