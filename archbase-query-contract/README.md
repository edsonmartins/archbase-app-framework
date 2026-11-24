# archbase-query-contract

Ponte entre a biblioteca `rsql-jpa-search` (`io.github.ggomarighetti`), que cuida do contrato de busca (campos
expostos, seletor público → caminho JPA, operadores permitidos, validação, paginação), e o motor RSQL do
[archbase-query](../archbase-query/README.md). Com ela, a `Specification` final é gerada por
`ArchbaseRSQLJPASupport` em vez do backend padrão da biblioteca.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-query-contract</artifactId>
    <version>3.5.0</version>
</dependency>
```

Nenhum starter do Archbase traz este módulo.

## O que traz

| Dependência | Observação |
|---|---|
| `archbase-query` 3.5.0 | Motor RSQL |
| `rsql-jpa-search-core` 2.0.0 | Traz também `rsql-jpa-search-api` e `rsql-jpa-search-rsql-spi`. **Não** traz auto-configuração Spring Boot da biblioteca |
| `spring-boot-autoconfigure` | `optional` |

## Auto-configuração

`ArchbaseRsqlContractAutoConfiguration`, registrada no `AutoConfiguration.imports`, ordenada antes de
`io.github.ggomarighetti.rsqljpasearch.autoconfigure.RsqlJpaSearchEngineAutoConfiguration`.

| Condição | O que registra |
|---|---|
| `RsqlBackendAdapter` no classpath, `archbase.query.contract.enabled` diferente de `false` e nenhum outro bean `RsqlBackendAdapter` | bean `archbaseRsqlBackendAdapter` (`ArchbaseRsqlBackendAdapter`) |

| Propriedade | Padrão | Efeito |
|---|---|---|
| `archbase.query.contract.enabled` | `true` | `false` não registra o adapter |

O bean só tem efeito se algo o consumir. A auto-configuração de engine da `rsql-jpa-search`
(`RsqlJpaSearchEngineAutoConfiguration`) não está nas dependências deste módulo; se a aplicação não a tiver, monte
o engine à mão, como abaixo.

## Uso

`ArchbaseRsqlBackendAdapter.compile(request)` chama
`ArchbaseRSQLJPASupport.toSpecification(request.rsql(), request.distinct(), request.definition().filteringPaths())`:
a string RSQL original é reanalisada pelo parser do Archbase, usando o mapa seletor público → caminho da definição.

Exemplo adaptado do teste do módulo (`ArchbaseRsqlBackendAdapterTest`):

```java
SearchDefinition<Product> definition = SearchDefinition.builder()
        .entity(Product.class)
        .fields(fields -> {
            fields.add("name", String.class, field -> field.filterable());
            fields.add("category", String.class, field -> field.path("category.name").filterable());
            fields.add("price", Integer.class, field -> field.filterable());
        })
        .build();

SearchRsqlEngine engine = SearchRsqlEngines.builder(new ArchbaseRsqlBackendAdapter())
        .conversionService(new DefaultConversionService())
        .build();
SearchCompiler compiler = new SearchCompiler(engine, SearchPolicy.defaults());

CompiledSearch<Product> compiled = compiler.compile("category==Perifericos;price=lt=150", null, pageable, definition);
Specification<Product> spec = compiled.specification();
```

O motor do Archbase precisa estar inicializado com os `EntityManager` (`new ArchbaseRSQLJPASupport(...)`), o que o
`archbase-starter-core` faz com `archbase.rsql.enabled=true` (padrão); sem isso, a `Specification` falha ao ser
aplicada. Ver [archbase-query](../archbase-query/README.md#auto-configuração).

## Limitações

- Só os operadores do Archbase (`br.com.archbase.query.rsql.common.RSQLOperators`) são reconhecidos. Operadores
  customizados registrados apenas na `rsql-jpa-search` falham no parser do Archbase.
- As listas de propriedade/operador, mapeamentos e seletores protegidos configurados em `RSQLCommonSupport` continuam
  valendo, somados às regras da definição.
- O `distinct` da requisição é repassado; ordenação e paginação ficam por conta da biblioteca.

## Referência

- [archbase-query](../archbase-query/README.md)
- Módulos do framework: [CLAUDE.md](../CLAUDE.md)
