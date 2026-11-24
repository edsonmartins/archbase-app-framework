# archbase-annotation-processor

Processador de anotações (APT) do Querydsl que gera as classes de consulta `Q<Classe>` a partir das anotações de
domínio do Archbase (`@DomainEntity`, `@DomainValueObject`), em vez das anotações JPA (`@Entity`,
`@Embeddable`). Serve para modelos de domínio que não carregam mapeamento JPA no código-fonte.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-annotation-processor</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

| Dependência | Para quê |
|---|---|
| `querydsl-apt` (`io.github.openfeign.querydsl`, 7.2) | Base do processador (`AbstractQuerydslProcessor`, `DefaultConfiguration`) |
| `archbase-domain-driven-design-spec` | As anotações de domínio |
| `jakarta.persistence-api` | Anotações JPA de propriedade lidas pelo processador |
| `guava` | Uso interno |

## Auto-configuração

Não tem: atua só na compilação. O jar registra `br.com.archbase.apt.querydsl.jpa.ArchbaseJPAAnnotationProcessor`
em `META-INF/services/javax.annotation.processing.Processor`.

A partir do JDK 23 o `javac` não executa processadores apenas por estarem no classpath; é preciso declará-los.
Com Java 25, a forma direta é o `annotationProcessorPaths` do `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>br.com.archbase</groupId>
                <artifactId>archbase-annotation-processor</artifactId>
                <version>3.5.0</version>
            </path>
            <!-- liste aqui também os outros processadores do projeto (Lombok etc.):
                 com annotationProcessorPaths, só os listados rodam -->
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

As classes geradas vão para `target/generated-sources/annotations`.

## O que o processador reconhece

`ArchbaseJPAAnnotationProcessor` cria um `ArchbaseJPAConfiguration` (subclasse de `DefaultConfiguration` do
Querydsl, palavras reservadas JPA, modo estrito ligado) com estes papéis:

| Papel no Querydsl | Anotação |
|---|---|
| Entidade (gera `Q<Classe>`) | `@DomainEntity` |
| Supertipo | `@jakarta.persistence.MappedSuperclass` |
| Embutível | `@DomainValueObject` |
| Embutido | `@DomainIdentifier` |
| Ignorar | `@DomainTransient` |

Além disso, como o processador padrão de JPA do Querydsl:

- `@Access` na classe decide entre campos (`FIELD`) e métodos (`PROPERTY`); sem ela, o lado que tiver anotações
  JPA/Querydsl de propriedade (`@Column`, `@Id`, `@ManyToOne`, `@Transient`, `@QueryType` e outras) é o usado;
- `targetEntity` de `@ManyToOne`, `@OneToOne`, `@OneToMany` e `@ManyToMany` substitui o tipo declarado;
- `@Temporal` define o tipo da propriedade (`DATE`, `TIME`, `DATETIME`), exceto em `@ElementCollection`.

## Exemplo

```java
@DomainEntity
public class Pedido {
    private String id;
    private String cliente;
    private BigDecimal total;
}
```

Gera `QPedido` no mesmo pacote, para uso com Querydsl:

```java
QPedido pedido = QPedido.pedido;
Predicate filtro = pedido.cliente.eq("ACME").and(pedido.total.gt(BigDecimal.TEN));
```

## Limitações

- Classes com `@Entity` mas sem `@DomainEntity` **não** geram `Q<Classe>` por este processador. Projeto com
  entidades JPA comuns usa o processador do próprio Querydsl (`querydsl-apt` com classificador `jakarta`).
- `@DomainIdentifier` e `@DomainTransient` são `@Target(TYPE)`: não podem ser postas em campo ou método. No
  Querydsl, os papéis "embutido" e "ignorar" são de propriedade, então na prática não há como marcar uma
  propriedade para ser ignorada ou tratada como embutida por meio delas; use `@QueryTransient` / `@QueryEmbedded`
  do Querydsl.
- O módulo é compilado com `maven-compiler-plugin` 3.5.1 e alvo Java 17, diferente do resto do framework (Java
  25). Roda normalmente num `javac` 25.
- O módulo não tem testes.

## Referência

- Anotações de domínio: `archbase-domain-driven-design-spec` (`br.com.archbase.ddd.domain.annotations`)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
