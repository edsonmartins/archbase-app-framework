# archbase-transformation

Dois plugins de transformação de bytecode do [Byte Buddy](https://bytebuddy.net) (`net.bytebuddy.build.Plugin`)
para rodar em tempo de build sobre as classes compiladas da aplicação: um põe estereótipos do Spring em classes
marcadas com anotações de domínio, o outro copia mapeamento JPA de uma classe de persistência para a entidade de
domínio correspondente.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-transformation</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

| Dependência | Para quê |
|---|---|
| `archbase-domain-driven-design-spec` | As anotações lidas pelos plugins; traz também `spring-context`, `jakarta.persistence-api` e `slf4j-api`, usados pelos plugins |
| `byte-buddy` | API de plugin e de transformação (versão do BOM do Spring Boot; 1.18.15 no build do 3.5.0) |

## Auto-configuração

Não tem. É só biblioteca de build: nada roda na aplicação em execução. Os plugins só têm efeito se a aplicação
configurar o `byte-buddy-maven-plugin` (ou equivalente) para aplicá-los.

## `ArchbaseTransformToSpring`

Aplica-se a classes que tenham uma das anotações de `br.com.archbase.ddd.domain.annotations` e acrescenta a
anotação do Spring correspondente, se a classe ainda não a tiver:

| Anotação de domínio | Anotação acrescentada |
|---|---|
| `@DomainService` | `@org.springframework.stereotype.Service` |
| `@DomainRepository` | `@org.springframework.stereotype.Repository` |
| `@DomainFactory` | `@org.springframework.stereotype.Component` |

O efeito é que as classes de domínio ficam sem dependência de Spring no código-fonte e ainda assim são
encontradas pelo component scan.

## `ArchbaseTransformToJPA`

Aplica-se a classes anotadas com `@PersistenceDomainEntity`, `@PersistenceDomainIdentifier` ou
`@PersistenceDomainValueObject` (`br.com.archbase.ddd.persistence.annotations`, retenção `CLASS`). Na prática,
só `@PersistenceDomainEntity` produz efeito:

- `@PersistenceDomainEntity(Cliente.class)` numa classe de modelo de persistência faz o plugin carregar a classe
  de domínio indicada (`Cliente`), refazê-la (`rebase`) com as anotações de tipo da classe de persistência e,
  para cada campo de mesmo nome nas duas, com as anotações de campo que a classe de domínio ainda não tem;
- a classe de domínio refeita é gravada **diretamente** no diretório de onde ela foi carregada
  (`getProtectionDomain().getCodeSource().getLocation()`), sobrescrevendo o `.class`. A classe de persistência
  em si não é alterada;
- erro de gravação só é impresso (`printStackTrace`); o build continua.

```java
// Classe de domínio, sem JPA
public class Cliente implements Identifiable<ClienteId> {
    private ClienteId id;
    private String nome;

    @Override
    public ClienteId getId() {
        return id;
    }
}

// Modelo de persistência: só carrega o mapeamento
@PersistenceDomainEntity(Cliente.class)
@Entity
@Table(name = "CLIENTE")
class ClientePersistence {
    @EmbeddedId
    private ClienteId id;

    @Column(name = "NOME")
    private String nome;
}
```

`@PersistenceDomainValueObject` e `@PersistenceDomainIdentifier` fazem a classe passar pelo filtro do plugin,
mas não alteram nada (ver limitações).

## Uso com o `byte-buddy-maven-plugin`

```xml
<plugin>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy-maven-plugin</artifactId>
    <!-- mesma versão do byte-buddy resolvido no projeto -->
    <version>${byte-buddy.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>transform</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <transformations>
            <transformation>
                <plugin>br.com.archbase.transformation.ArchbaseTransformToSpring</plugin>
            </transformation>
        </transformations>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>br.com.archbase</groupId>
            <artifactId>archbase-transformation</artifactId>
            <version>3.5.0</version>
        </dependency>
    </dependencies>
</plugin>
```

`${byte-buddy.version}` existe quando o projeto herda do `spring-boot-starter-parent`; fora dele, informe a
versão.

## Limitações

- Não há testes no módulo, nem uso dentro do próprio framework. O comportamento descrito vem da leitura do
  código, não de uma execução verificada.
- `ArchbaseTransformToJPA`:
  - o tratamento de `@PersistenceDomainValueObject` e `@PersistenceDomainIdentifier` (acrescentar
    `@Embeddable` e, no primeiro caso, `Serializable`) nunca executa: o código testa se a classe é *atribuível*
    ao tipo da anotação, o que uma classe comum nunca é;
  - copia para a classe de domínio todas as anotações de tipo da classe de persistência, inclusive as que a
    classe de domínio já tem (anotação repetida) e a própria `@PersistenceDomainEntity`; só deixa de copiar as
    três anotações `PersistenceDomain*` quando a classe de domínio não as tem;
  - grava fora do fluxo normal do Byte Buddy, no local de onde a classe de domínio foi carregada; se ela estiver
    num jar, a gravação falha e só gera stack trace;
  - registra em `INFO` o nome de toda classe examinada.
- `ArchbaseTransformToSpring` não acrescenta nada além dos três estereótipos acima.

## Referência

- Anotações de domínio e de persistência: `archbase-domain-driven-design-spec`
  (`br.com.archbase.ddd.domain.annotations`, `br.com.archbase.ddd.persistence.annotations`)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
