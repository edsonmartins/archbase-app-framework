# archbase-architecture

Anotações para declarar o estilo arquitetural do código: camadas, onion (clássica e simplificada) e hexagonal. O
jar só tem anotações e um arquivo de metadados; nenhuma dependência, nenhum comportamento em tempo de execução.
Quem verifica as anotações são as regras de [`archbase-architecture-rules`](../archbase-architecture-rules/README.md).

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-architecture</artifactId>
    <version>3.5.0</version>
</dependency>
```

## Auto-configuração

Não tem.

## Anotações

Todas têm `@Retention(RUNTIME)` e `@Target({PACKAGE, TYPE})`: vão no tipo ou no `package-info.java`.

| Estilo | Pacote | Anotações (de cima para baixo / de fora para dentro) |
|---|---|---|
| Camadas | `br.com.archbase.layered` | `@InterfaceLayer`, `@ApplicationLayer`, `@DomainLayer`, `@InfrastructureLayer` |
| Onion clássica | `br.com.archbase.onion.classical` | `@InfrastructureRing`, `@ApplicationServiceRing`, `@DomainServiceRing`, `@DomainModelRing` |
| Onion simplificada | `br.com.archbase.onion.simplified` | `@InfrastructureRing`, `@ApplicationRing`, `@DomainRing` |
| Hexagonal | `br.com.archbase.hexagonal` | `@Application`, `@Port`, `@PrimaryPort`, `@SecondaryPort`, `@Adapter`, `@PrimaryAdapter`, `@SecondaryAdapter` |

As anotações hexagonais têm os atributos opcionais `name` e `description` (padrão `""`); as demais não têm
atributos. `@PrimaryPort` e `@SecondaryPort` são meta-anotadas com `@Port`; `@PrimaryAdapter` e
`@SecondaryAdapter`, com `@Adapter`.

Anotar o pacote raiz de cada camada é o uso esperado; o tipo herda a anotação do pacote mais próximo:

```java
// src/main/java/com/acme/app/domain/package-info.java
@DomainLayer
package com.acme.app.domain;

import br.com.archbase.layered.DomainLayer;
```

```java
@PrimaryPort(description = "Cadastro de clientes")
public interface CadastrarCliente { ... }
```

## Metadados para ferramentas

`META-INF/jmolecules-stereotypes.json` descreve as anotações como estereótipos, em quatro grupos
(`archbase.architecture.hexagonal`, `.layered`, `.onion.classical`, `.onion.simplified`), no formato de estereótipos
do jMolecules, para ferramentas que leem esse arquivo. O framework não o lê.

## Limitações

- Sem `archbase-architecture-rules` (ou regra própria), as anotações são só documentação: nada impede uma
  dependência proibida.

## Referência

- [`archbase-architecture-rules`](../archbase-architecture-rules/README.md): `ArchbaseArchitectureStyleRules` e
  `withArchitectureStyleRules(...)`.
