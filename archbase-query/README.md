# archbase-query

Filtros RSQL para JPA: parser próprio (gerado por JavaCC), conversão de RSQL em `Specification` do Spring Data JPA
ou em `BooleanExpression` do QueryDSL, controle de quais propriedades e operadores o cliente pode usar, proteção de
seletores contra `OR` e limite de tamanho de página.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-query</artifactId>
    <version>3.5.0</version>
</dependency>
```

Chega pelo `archbase-domain-driven-design`, portanto por `archbase-starter-core` e `archbase-starter`.

## O que traz

`spring-boot-starter-data-jpa`, `querydsl-core` (`io.github.openfeign.querydsl`), `spring-security-core`,
`aspectjweaver` e `commons-text`.

## Auto-configuração

O módulo não tem auto-configuração nem lê propriedades. Toda a configuração é **estática** (mapas globais em
`RSQLCommonSupport`) e só funciona depois que alguém cria um `ArchbaseRSQLJPASupport` com os `EntityManager` da
aplicação; sem isso, a conversão falha com `IllegalStateException` ("Nenhum bean de gerenciador de entidade...").

Com o `archbase-starter-core`, o `ArchbaseRSQLConfiguration` faz isso:

| Propriedade (lida pelo starter) | Padrão | Efeito |
|---|---|---|
| `archbase.rsql.enabled` | `true` | Cria o bean `ArchbaseRSQLJPASupport` com todos os `EntityManager` do contexto |
| `archbase.rsql.max-page-size` | `1000` | Chama `RSQLCommonSupport.setMaxPageSize`; `<= 0` desliga o limite |

O starter também chama `RSQLCommonSupport.clear()` quando o contexto fecha. Ver
[archbase-starter-core](../archbase-starter-core/README.md#auto-configuração).

Sem o starter:

```java
@Bean
ArchbaseRSQLJPASupport rsqlSupport(ApplicationContext ctx) {
    return new ArchbaseRSQLJPASupport(ctx.getBeansOfType(EntityManager.class));
}
```

## Sintaxe

`;` (ou `and`) é E, `,` (ou `or`) é OU; o E tem precedência; parênteses agrupam. Valores com espaço ou caractere
reservado vão entre aspas.

| Operador | Sinônimos | Efeito |
|---|---|---|
| `==` | | Igual. Em `String`, `*` vira curinga (`like`) e `^` torna a comparação insensível a maiúsculas |
| `!=` | | Diferente, com as mesmas regras de `*` e `^` |
| `=gt=`, `=ge=`, `=lt=`, `=le=` | `>`, `>=`, `<`, `<=` | Comparação; só para tipos `Comparable` |
| `=in=`, `=out=` | | Lista: `status=in=(A,B)` |
| `=bt=`, `=nb=` | `=between=`, `=notbetween=` | Exatamente dois valores: `preco=bt=(10,20)` |
| `=na=`, `=nn=` | `=isnull=`/`=null=`, `=notnull=`/`=isnotnull=` | Nulo / não nulo (o valor é ignorado, mas obrigatório) |
| `=ke=`, `=nk=` | `=like=`, `=notlike=` | `like '%valor%'` |
| `=ic=` | `=icase=` | Igual, sem diferenciar maiúsculas |
| `=ik=`, `=ni=` | `=ilike=`, `=inotlike=` | `like` sem diferenciar maiúsculas |

Seletores aceitam caminho com ponto (`categoria.nome`): associações viram `LEFT JOIN`; `@Embedded` e
`@ElementCollection` também são percorridos. O seletor precisa terminar num atributo básico.

Valores são convertidos para o tipo do atributo: primeiro pelo `ConversionService` do módulo, depois por regras
próprias (`UUID`, `LocalDate`, `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`, `Date` no formato ISO, booleano
`true`/`false`/`1`/`0`, enum pelo nome, ou construtor `(String)`). Valor que não converte vira `null`; em `==`/`!=`
sobre tipo não `String`, a condição vira falsa (e é registrado WARN).

## Uso

### Specification

```java
Specification<Cliente> spec = ArchbaseRSQLJPASupport.toSpecification("nome==Ana*;idade>18");
Page<Cliente> pagina = repository.findAll(spec, pageable);
```

| Método estático de `ArchbaseRSQLJPASupport` | Observação |
|---|---|
| `toSpecification(rsql)` / `rsql(rsql)` | `rsql(...)` são sinônimos com as mesmas sobrecargas |
| `toSpecification(rsql, distinct)` | Aplica `query.distinct(distinct)` |
| `toSpecification(rsql, Map<String,String> propertyPathMapper)` | Seletor público → caminho JPA, só para esta consulta |
| `toSpecification(rsql, List<RSQLCustomPredicate<?>>)` | Operadores próprios (abaixo) |
| `toSort(sort)`, `toSort(sort, propertyPathMapper)` | Ordenação no formato `campo:asc;outro.campo:desc` |
| `findAll`, `findOne`, `count` (recebem `JpaSpecificationExecutor` e a string RSQL) | Atalhos sem tipo genérico (`List`, `Page`, `Optional` crus) |

String vazia ou `null` gera `Specification` sem filtro. Erro de sintaxe lança `RSQLParserException`; propriedade
desconhecida, operador inválido para o tipo ou acesso negado lançam `IllegalArgumentException`.

`RSQLCommonSupport.toMultiValueMap(rsql)` e `toComplexMultiValueMap(rsql)` analisam a string sem consultar o banco e
devolvem seletor → valores (o segundo, seletor → símbolo do operador → valores), ignorando a estrutura E/OU.

### QueryDSL

```java
BooleanExpression filtro = ArchbaseRSQLQueryDslSupport.toPredicate("nome==Ana", QCliente.cliente);
```

Também aceita `propertyPathMapper`. As classes `Q*` são geradas pela aplicação.

### Operador próprio

```java
RSQLCustomPredicate<String> naoVazio = new RSQLCustomPredicate<>(
        new ComparisonOperator("=naovazio="), String.class,
        in -> in.getCriteriaBuilder().notEqual(in.getPath(), ""));
List<RSQLCustomPredicate<?>> custom = List.of(naoVazio);

Specification<Cliente> spec = ArchbaseRSQLJPASupport.toSpecification("email=naovazio=x", custom);
```

O símbolo precisa casar com `=[a-zA-Z]*=`. Os argumentos chegam convertidos para o `type` informado.

## Configuração global

Métodos estáticos de `RSQLCommonSupport` (herdados por `ArchbaseRSQLJPASupport`), em geral chamados uma vez na
inicialização:

| Método | Efeito |
|---|---|
| `addMapping(Entidade.class, "seletor", "propriedade")` / `addMapping(Entidade.class, Map)` | Apelido de propriedade por entidade, aplicado em todas as consultas |
| `addPropertyWhitelist(Entidade.class, "prop")` / `(…, List)` | Com lista definida, só essas propriedades da entidade podem ser filtradas |
| `addPropertyBlacklist(Entidade.class, "prop")` / `(…, List)` | Propriedades proibidas |
| `addOperatorWhitelist(Entidade.class, "prop", ops...)` | Com lista definida, só esses operadores na propriedade |
| `addOperatorBlacklist(Entidade.class, "prop", ops...)` | Operadores proibidos na propriedade (ex.: `RSQLOperators.LIKE`) |
| `addProtectedSelector(Entidade.class, "sel"...)` | O seletor não pode aparecer dentro de um `OR` (ver abaixo) |
| `addConverter(Tipo.class, Converter<String, Tipo>)`, `addEntityAttributeParser(Tipo.class, Function)` | Conversão de valor para um tipo |
| `addEntityAttributeTypeMap(Tipo.class, OutroTipo.class)` | Trata atributos de `Tipo` como `OutroTipo` na conversão |
| `setMaxPageSize(int)` | Limite usado por `ArchbasePageableGuard` |
| `clear()` | Apaga mapeamentos, listas, seletores protegidos e o cache de metamodelo (não os conversores nem o limite de página) |

Listas de propriedade e de operador valem para a **classe que declara o atributo final**. Em `categoria.nome`, conta
a configuração de `Categoria`, não a da entidade raiz: uma whitelist só na raiz não impede filtrar por atributos de
entidades associadas.

### Seletores protegidos

`RSQLOrBypassAnalyzer` rejeita (`IllegalArgumentException`) uma consulta em que um seletor protegido aparece sob um
`OR`, para que `tenantId==X;nome==a,nome==b` — lido como `(tenantId==X;nome==a),nome==b` — não escape do filtro.
Sem seletor registrado para a entidade, não faz nada. A verificação usa o seletor como escrito na consulta, antes de
qualquer mapeamento. O isolamento por tenant do Archbase não depende disso: é feito pelo `@TenantId` do Hibernate
(ver [archbase-multitenancy](../archbase-multitenancy/README.md)).

### Tamanho de página

`ArchbasePageableGuard.guard(pageable)` devolve um `PageRequest` com o tamanho reduzido ao máximo (e WARN) quando o
pedido excede `RSQLCommonSupport.getMaxPageSize()`; `clampSize(int)` faz o mesmo com um número. Não recusa a
requisição. Os controllers de consulta do `archbase-domain-driven-design` chamam `clampSize`; consultas que a
aplicação monta por conta própria precisam chamar o guard.

## Limitações

- **Estado global por JVM.** Mapeamentos, listas e conversores são estáticos: valem para todos os contextos Spring
  da JVM e persistem entre testes até `clear()`.
- **QueryDSL não verifica seletores protegidos.** `ArchbaseRSQLQueryDslSupport.toPredicate` aplica listas de
  propriedade e de operador, mas não chama `RSQLOrBypassAnalyzer`.
- `toSort("campo1:asc;campo2,desc;assoc.campo3")`: direção depois de `:` ou `,`, ASC quando omitida. Passa pela
  **lista negra** (em cada segmento) e pelo `propertyPathMapper`, mas não pela lista branca nem por `addMapping`. Até a
  3.5.0 não passava por controle nenhum, só `:` funcionava e um item sem direção lançava
  `ArrayIndexOutOfBoundsException`.
- `=ke=` e afins não escapam `%` e `_` do valor: o cliente pode usá-los como curinga.
- Montar joins e checar acesso registra linhas em INFO a cada consulta (`join(...)`, `accessControl(...)`).
- **Row-level security não está implementado.** O pacote `br.com.archbase.query.rls.support` tem `@DynamicFilter`,
  `RowLevelSecurityFilter` e `ArchbaseRowLevelSecurityFilterEntityLocator`, mas nada os registra nem os chama:
  `ArchbaseQueryMonitor` está inteiramente comentado e o localizador não é bean. Anotar uma entidade com
  `@DynamicFilter` não filtra nada.

## Referência

- Ponte com a biblioteca `rsql-jpa-search`: [archbase-query-contract](../archbase-query-contract/README.md)
- [archbase-starter-core](../archbase-starter-core/README.md)
- Módulos do framework: [CLAUDE.md](../CLAUDE.md)
