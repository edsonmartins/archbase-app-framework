# archbase-domain-driven-design-spec

Contratos e tipos base de DDD do Archbase: bases de entidade JPA (`PersistenceEntityBase`,
`TenantPersistenceEntityBase`, `CompanyPersistenceEntityBase`), contratos de domínio (`AggregateRoot`, `Entity`,
`Repository`, `Association`, `ValueObject`), a especificação em memória (`ArchbaseSpecification`), identificadores
tipados, anotações de estereótipo e o contexto de tenant (`ArchbaseTenantContext`).

É só biblioteca: não tem auto-configuração, bean nem propriedade. A implementação JPA dos repositórios está no
[`archbase-domain-driven-design`](../archbase-domain-driven-design/README.md).

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-domain-driven-design-spec</artifactId>
    <version>3.5.0</version>
</dependency>
```

Vem transitivamente por quase todos os módulos (`archbase-domain-driven-design`, `archbase-starter-core`,
`archbase-shared-kernel`, `archbase-value-objects`...).

## O que traz

`jakarta.persistence-api`, `hibernate-core`, `querydsl-core`, `spring-data-commons`, `spring-core`,
`spring-context`, `slf4j-api` e `swagger-annotations-jakarta`. O `spring-data-jpa` é `provided`: as bases de entidade
usam o `AuditingEntityListener` dele, então a aplicação precisa tê-lo (o `archbase-starter-core` traz).

## Bases de entidade JPA (`br.com.archbase.ddd.domain.base`)

Todas são `@MappedSuperclass` com id `String` (UUID gerado no construtor), `version = 1` e `createEntityDate =
now()` no construtor.

| Classe | Colunas | Isolamento |
|---|---|---|
| `PersistenceEntityBase` | `ID`, `CODIGO`, `VERSAO` (`@Version`), `DH_CRIACAO`, `USUARIO_CRIOU`, `DH_ATUALIZACAO`, `ULTIMO_USUARIO_ALTEROU` | nenhum |
| `TenantPersistenceEntityBase` (estende a anterior) | as mesmas + `TENANT_ID` (`@TenantId`, 40) | por tenant |
| `CompanyPersistenceEntityBase` (**não** estende as anteriores) | `ID`, `CODE`, `VERSION`, `CREATION_DATE`, `CREATED_BY`, `UPDATE_DATE`, `LAST_MODIFIED_BY`, `TENANT_ID` (`@TenantId`), `COMPANY_ID` | por tenant e empresa |

```java
@Entity
@Table(name = "PEDIDO")
public class Pedido extends TenantPersistenceEntityBase {
    @Column(name = "NUMERO")
    private String numero;
}
```

O que acontece de fato com as colunas de auditoria:

- `PersistenceEntityBase` tem `@EntityListeners(AuditingEntityListener.class)`, mas só `USUARIO_CRIOU`
  (`@CreatedBy`) e `ULTIMO_USUARIO_ALTEROU` (`@LastModifiedBy`) são anotadas. Elas só são preenchidas com
  `@EnableJpaAuditing` e um `AuditorAware` (o `archbase-starter-core` liga os dois).
- `DH_CRIACAO` recebe `now()` no construtor, não por auditoria. `DH_ATUALIZACAO` nunca é preenchida
  automaticamente.
- `CompanyPersistenceEntityBase` usa outros nomes de coluna e não tem o `AuditingEntityListener`: usuário de criação
  e de alteração não são preenchidos.

### Empresa

`CompanyPersistenceEntityBase` declara o filtro Hibernate `companyFilter` (`COMPANY_ID = :companyId`) e o listener
`CompanyEntityListener`, que copia `ArchbaseTenantContext.getCompanyId()` para `companyId` em `@PrePersist`,
`@PreUpdate` e `@PreRemove`. Sem empresa no contexto, grava `null` — inclusive ao alterar um registro que já tinha
empresa. Quem liga o filtro nas consultas é o aspecto do [`archbase-multitenancy`](../archbase-multitenancy/README.md#isolamento-por-empresa).

## Contexto de tenant

`br.com.archbase.ddd.context.ArchbaseTenantContext`: tenant e empresa da thread em `InheritableThreadLocal`
(`setTenantId`, `getTenantId`, `setCompanyId`, `getCompanyId`, `clear`). Constantes `X_TENANT_ID`, `X_COMPANY_ID` e
`COMPANY_DISCARDED_ATTRIBUTE`. Quem preenche e limpa é o [`archbase-multitenancy`](../archbase-multitenancy/README.md).

## Modelo de domínio (sem JPA)

- `DomainEntityBase<T extends AggregateRoot<T, Identifier>>` e `DomainAggregatorBase<T ...>`: classes abstratas com
  id `ArchbaseIdentifier`, campos de auditoria, `createdBy(SecurityUser)`, `modifiedBy(SecurityUser)` e
  `abstract ValidationResult validate()` (o `contracts.ValidationResult`). `DomainEntityBase` tem `equals`/`hashCode`
  pelo id; `DomainAggregatorBase` não.
- `ArchbaseIdentifier`: `Identifier` com um `String` (UUID por padrão).
- Contratos (`br.com.archbase.ddd.domain.contracts`): `Identifier`, `Identifiable`, `Entity<T, ID>`,
  `AggregateRoot<T, ID>`, `ValueObject` (marcador, `Serializable`), `Association` (`forId`, `forAggregate`,
  `pointsTo`), `AggregateLookup`, `InsideAssociationResolver`, `Event`, `EventPublisher`, `EventHandler`,
  `SecurityUser`, `ValidationResult`/`ValidationError`, e portas (`EntityPersistencePort`,
  `CreateOrUpdateOrRemoveEntityUseCase`, `FindDataWithFilterQuery`, `ArchbaseStoragePort`).
- `Repository<T, ID, N>`: interface Spring Data (`PagingAndSortingRepository`, `CrudRepository`,
  `QuerydslPredicateExecutor`, `RevisionRepository`) com `matching`/`howMany`/`containsAny`
  (`ArchbaseSpecification`) e `findAll(String filter, Pageable)`. É a base de `ArchbaseCommonJpaRepository`.

```java
public record PedidoId(UUID value) implements Identifier { }

public class Pedido implements AggregateRoot<Pedido, PedidoId> {
    private PedidoId id;
    private Association<Cliente, ClienteId> cliente;   // referência a outro agregado por id
    @Override public PedidoId getId() { return id; }
}
```

## Especificação em memória (`br.com.archbase.ddd.domain.specification`)

`ArchbaseSpecification<T>` tem um método, `isSatisfiedBy(T)`. Para compor, implemente
`AbstractArchbaseSpecification<T>` ou `ComposableArchbaseSpecification<T>` (interfaces com `and`, `or`, `not`
padrão) ou use `Specifications.and/or/not`. `Specifications` também tem `isSatisfiedByAll`, `isSatisfiedBySome`,
`findAllSatisfying` e `countAllSatisfying`. `ConjunctionSpecification` recebe uma lista e
`DisjunctionSpecification`, varargs. `ValueBoundArchbaseSpecification` compara o valor de um campo, lido por reflexão.

```java
public class ClienteAtivo implements AbstractArchbaseSpecification<Cliente> {
    @Override
    public boolean isSatisfiedBy(Cliente c) {
        return c.isAtivo();
    }
}

List<Cliente> ativos = Specifications.findAllSatisfying(new ClienteAtivo().and(new ClienteVip()), clientes);
```

Não é `org.springframework.data.jpa.domain.Specification`. Para filtro no banco, use RSQL
([`archbase-query`](../archbase-query/README.md)) ou `Specification` do JPA.

## Identificadores tipados (`br.com.archbase.ddd.domain.identifiers`)

Para `record` de um componente que implementa `Identifier` (desde a 3.4.6). `ArchbaseIdentifier` e outros
identificadores não são afetados.

- `Identifiers`: `isSingleValue`, `valueType`, `valueOf`, `of(tipo, valor)`.
- `AbstractIdentifierAttributeConverter<ID, V>`: base de conversor JPA; declare uma subclasse concreta por tipo:

  ```java
  @Converter(autoApply = true)
  public class PedidoIdConverter extends AbstractIdentifierAttributeConverter<PedidoId, UUID> { }
  ```

- `IdentifierConverters.register(ConverterRegistry)`: conversores Spring (por exemplo, `@PathVariable PedidoId`).
  Nada no framework o chama; a aplicação registra, por exemplo, num `WebMvcConfigurer.addFormatters`.
- O módulo Jackson correspondente (`ArchbaseIdentifierJacksonModule`) está no `archbase-domain-driven-design`.

## Eventos

`@DomainEvent(namespace, name)` e `@Externalized(value, target)` marcam tipos de evento; `DomainEventMetadata` lê
essas anotações (`namespace`, `name`, `qualifiedName`, `isExternalized`, `externalTarget`). Nenhum módulo do
framework usa `DomainEventMetadata` nem `@Externalized` hoje: marcar um evento como externalizado não o publica em
lugar nenhum.

## Anotações de estereótipo

`br.com.archbase.ddd.domain.annotations`: `@DomainAggregateRoot`, `@DomainEntity`, `@DomainValueObject`,
`@DomainIdentifier`, `@DomainRepository`, `@DomainService`, `@DomainFactory`, `@DomainPolicy`, `@DomainModule`,
`@DomainBoundedContext`, `@DomainCommand`, `@DomainQuery`, `@DomainEventListener` etc. São documentação; o arquivo
`META-INF/jmolecules-stereotypes.json` as expõe a ferramentas que leem estereótipos jMolecules. Regras ArchUnit que
as verificam estão no `archbase-architecture-rules`.

Também há `@StorageField` (`domain.aspect.annotations`), `@RequestBodyDTO`/`@ResponseBodyDTO` (`dto.annotations`) e
`@PersistenceDomainEntity`/`@PersistenceField`/... (`persistence.annotations`). `@StorageField` só teria efeito pelo
`ArchbaseStorageFieldAspect`, que não está registrado (ver o README do
[`archbase-domain-driven-design`](../archbase-domain-driven-design/README.md#limitações)).

## Limitações

- `DomainEntityBase` e `DomainAggregatorBase` exigem `T extends AggregateRoot<T, Identifier>`: o tipo do id do
  agregado tem de ser exatamente `Identifier`, não um identificador tipado.
- Há dois `ValueObject` e dois `ValidationResult` no framework: os deste módulo (`contracts`) e os de
  [`archbase-value-objects`](../archbase-value-objects/README.md) / [`archbase-shared-kernel`](../archbase-shared-kernel/README.md).
  Não são intercambiáveis.

## Referência

- Repositórios JPA: [archbase-domain-driven-design](../archbase-domain-driven-design/README.md)
- Tenant e empresa: [archbase-multitenancy](../archbase-multitenancy/README.md)
- Varredura e auditoria JPA: [archbase-starter-core](../archbase-starter-core/README.md)
- Visão geral do framework: [CLAUDE.md](../CLAUDE.md)
