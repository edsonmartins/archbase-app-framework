# archbase-starter-hypersistence

Starter do [archbase-hypersistence](../archbase-hypersistence/README.md): traz o Hypersistence Utils (tipos JSON,
array, range, TSID) e as propriedades `archbase.hypersistence.*`.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-hypersistence</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

`archbase-hypersistence` (e com ele `hypersistence-utils-hibernate-73` e Jackson 2), `spring-boot-starter-data-jpa`,
`spring-boot-hibernate` e `spring-boot-autoconfigure`. O driver PostgreSQL não vem: declare-o na aplicação.

## O que é preciso ligar

Nada. Os tipos do Hypersistence Utils são anotações Hibernate na entidade (`@Type(JsonType.class)` etc.) e
funcionam assim que o jar está no classpath — inclusive sem este starter, só com `archbase-hypersistence`. O que o
starter acrescenta é pouco: o registro das propriedades e um log das opções na subida.

## Auto-configuração

Registrada: `ArchbaseHypersistenceAutoConfiguration`, depois de `HibernateJpaAutoConfiguration`.

| Condição | |
|---|---|
| `@ConditionalOnClass(JsonType.class)` | sempre verdadeira com este starter |
| `archbase.hypersistence.enabled` | padrão `true` |

Registra:

- `ArchbaseHypersistenceProperties` (via `@EnableConfigurationProperties`) e importa
  `ArchbaseHypersistenceConfiguration`, que só repete esse registro;
- um bean `archbaseHypersistenceProperties` do mesmo tipo, que é a mesma instância e serve para logar as opções
  em `INFO`. Efeito colateral: ficam **dois** beans `ArchbaseHypersistenceProperties` no contexto, e injetar esse
  tipo falha por ambiguidade, a menos que o ponto de injeção se chame `archbaseHypersistenceProperties` ou use
  `@Qualifier`.

Não registra tipo Hibernate, `TypeContributor`, repositório nem `DataSource`.

## Propriedades

| Propriedade | Padrão | Efeito |
|---|---|---|
| `archbase.hypersistence.enabled` | `true` | `false` desliga a auto-configuração (só o registro das propriedades e o log). Os tipos continuam funcionando |
| `archbase.hypersistence.json.enabled` | `true` | Nenhum: só aparece no log |
| `archbase.hypersistence.postgresql.array-types-enabled` | `true` | Nenhum: só aparece no log |
| `archbase.hypersistence.postgresql.range-types-enabled` | `true` | Nenhum: só aparece no log |
| `archbase.hypersistence.postgresql.hstore-enabled` | `false` | Nenhum |
| `archbase.hypersistence.postgresql.inet-enabled` | `false` | Nenhum |
| `archbase.hypersistence.repository.enhanced-methods-enabled` | `false` | Nenhum: só aparece no log |

Nenhum código lê essas opções para ligar ou desligar algo: pôr `json.enabled=false` não impede o uso de
`JsonType`, e `enhanced-methods-enabled=true` não acrescenta métodos a repositório nenhum.

## Uso

Exemplos de entidade, TSID e contagem de SQL estão no [README do módulo](../archbase-hypersistence/README.md#uso),
junto com a limitação do `ArchbaseHypersistenceRepository`, que este starter não resolve.

## Referência

- Módulo: [archbase-hypersistence](../archbase-hypersistence/README.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
