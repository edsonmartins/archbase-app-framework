# archbase-codegen-maven-plugin

Plugin Maven que gera código-fonte a partir das entidades do projeto: interfaces de repositório
(`ArchbaseCommonJpaRepository`), classes de serviço (`CommonArchbaseService`) e controllers REST
(`CommonArchbaseRestController`). Gera os arquivos `.java` uma vez, em `src/main/java`; não é um gerador
contínuo de `target/generated-sources`.

```xml
<plugin>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-codegen-maven-plugin</artifactId>
    <version>3.5.0</version>
</plugin>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

O prefixo do plugin é `archbase-codegen`. Com o plugin declarado no `pom.xml` do projeto, os goals rodam como
`mvn archbase-codegen:<goal>`.

## Goals

| Goal | Gera | Parâmetros obrigatórios | Filtro |
|---|---|---|---|
| `repositories` | `<Entidade><repository-postfix>.java` em `repository-package` | `entity-package`, `repository-package` | todas as entidades candidatas |
| `services` | `<Entidade><service-postfix>.java` em `service-package` | `entity-package`, `repository-package`, `service-package` | só entidades que já têm `<Entidade><repository-postfix>.java` em `repository-package` |
| `resources` | `<Entidade><resource-postfix>.java` em `resource-package` | `entity-package`, `repository-package`, `service-package`, `resource-package` | só entidades que já têm `<Entidade><service-postfix>.java` em `service-package` |

Por isso a ordem é `repositories`, depois `services`, depois `resources`. Entidade sem o arquivo da etapa
anterior é pulada com um aviso.

Os três goals têm `@Execute(phase = COMPILE)`: cada execução força um ciclo até `compile` antes de rodar,
porque as entidades são lidas das classes compiladas (classpath de runtime do projeto). Exigem projeto
(`requiresProject`) e resolução de dependências de compilação. Nenhum deles tem fase padrão de ligação; a forma
prevista de uso é a linha de comando.

Parâmetro obrigatório ausente encerra o goal com erro (`<parâmetro> configuração não encontrada!`); a
obrigatoriedade é conferida em código, e não pelo descritor do plugin, onde todos aparecem como opcionais.

## Parâmetros

Comuns aos três goals (declarados em `CommonsMojo`). O nome do elemento XML é o `name` do `@Parameter`, com
hífen. Nenhum parâmetro tem `property`: não há propriedade `-D` para eles, só configuração no `pom.xml`.

| Parâmetro | Tipo | Padrão | Efeito |
|---|---|---|---|
| `entity-package` | `String[]` | — | Pacotes onde as entidades são procuradas (inclui subpacotes) |
| `repository-package` | `String` | — | Pacote dos repositórios gerados; em `services`, onde o repositório é procurado |
| `repository-postfix` | `String` | `Repository` | Sufixo do nome do repositório |
| `service-package` | `String` | — | Pacote dos serviços gerados; em `resources`, onde o serviço é procurado |
| `service-postfix` | `String` | `Service` | Sufixo do nome do serviço |
| `resource-package` | `String` | — | Pacote dos controllers gerados |
| `resource-postfix` | `String` | `Resource` | Sufixo do nome do controller |
| `only-annotations` | `Boolean` | `false` | `true`: só entidades com `@ArchbaseDataGenerate`; `false`: também as com `@DomainEntity` |
| `overwrite` | `Boolean` | `false` | `true` apaga e regrava arquivos que já existem; `false` mantém o arquivo existente |
| `api-version` | `String` | `1` | Só o goal `resources` usa: entra no caminho `/api.v<api-version>/...` |
| `revision-number-class` | `String` | `java.lang.Integer` | Terceiro parâmetro de tipo (`N`, tipo da revisão) de repositório, serviço e controller |
| `additional-extends` | `String[]` | — | Só o goal `repositories` usa: interfaces extras que o repositório estende, cada uma com nome qualificado (sem ponto é erro) e um único parâmetro de tipo, a entidade |
| `dto-package` | `String` | — | Declarado, sem efeito: nenhum goal o lê |
| `dto-postfix` | `String` | `DTO` | Declarado, sem efeito |

O goal `repositories` declara ainda `classpathElements` (`${project.compileClasspathElements}`), que também não
é usado.

## Quais classes viram candidatas

A varredura percorre `entity-package` com um `ClassPathScanningCandidateComponentProvider` do Spring e aceita
classes de nível superior que tenham:

- `@ArchbaseDataGenerate` (`br.com.archbase.maven.plugin.codegen.annotation`), sempre;
- `@DomainEntity` (`br.com.archbase.ddd.domain.annotations`), quando `only-annotations=false`.

`@ArchbaseNoDataGenerate` exclui a classe. Uma entidade JPA anotada só com `@Entity` **não** é candidata: marque-a
com `@ArchbaseDataGenerate`. Para usar as duas anotações, a aplicação precisa do jar do plugin no classpath de
compilação:

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-codegen-maven-plugin</artifactId>
    <version>3.5.0</version>
    <optional>true</optional>
</dependency>
```

Para cada candidata, o tipo do identificador é o do primeiro campo ou método com `@Id` ou `@EmbeddedId`
encontrado subindo a hierarquia (em `PersistenceEntityBase`, `String`). Sem `@Id`, a entidade é pulada com erro
`Primary key not found`. Entidade num subpacote de `entity-package` gera o arquivo no subpacote correspondente
do pacote de destino.

## Exemplo

```xml
<plugin>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-codegen-maven-plugin</artifactId>
    <version>3.5.0</version>
    <configuration>
        <entity-package>
            <param>com.acme.vendas.domain</param>
        </entity-package>
        <repository-package>com.acme.vendas.repository</repository-package>
        <service-package>com.acme.vendas.service</service-package>
        <resource-package>com.acme.vendas.resource</resource-package>
        <only-annotations>true</only-annotations>
    </configuration>
</plugin>
```

```java
@Entity
@Table(name = "CLIENTE")
@ArchbaseDataGenerate
public class Cliente extends PersistenceEntityBase {
    private String nome;
}
```

```bash
# rode no diretório do módulo que contém as entidades
mvn archbase-codegen:repositories
mvn archbase-codegen:services
mvn archbase-codegen:resources
```

Resultado, para `Cliente` (formato abreviado):

- `ClienteRepository`: `@Repository public interface ClienteRepository extends ArchbaseCommonJpaRepository<Cliente,String,Integer>`.
- `ClienteService`: `@Service public class ClienteService extends CommonArchbaseService<Cliente,String,Integer>`,
  com construtor `@Autowired` que recebe o `ClienteRepository` e chama `super(...)`.
- `ClienteResource`: `@RestController`, `@RequestMapping(value = "/api.v1/cliente")`, `@CrossOrigin(origins = "*")`,
  `@Logging`, `@Api(value="Cliente")`, estende `CommonArchbaseRestController<Cliente,String,Integer>` e
  implementa `getService`, `getRepository`, `createConcretePage` e `createConcreteList`.

Ao final, o plugin imprime no log uma tabela com cada arquivo e o resultado.

## Limitações

- **O controller gerado não compila sem Swagger 2.** Ele importa `io.swagger.annotations.Api` e anota a classe
  com `@Api`. O Archbase traz `swagger-annotations-jakarta` (pacote `io.swagger.v3.oas.annotations`), não o
  Swagger 2: remova a anotação e o import, ou adicione `io.swagger:swagger-annotations` ao projeto.
- O controller gerado usa `@CrossOrigin(origins = "*")` e o caminho `/api.v<versão>/` (com ponto). Revise ambos
  antes de usar.
- Os arquivos são escritos em `<diretório atual>/src/main/java`, não no `basedir` do projeto. Em projeto
  multi-módulo, rodar a partir da raiz grava na raiz: rode no diretório do módulo.
- Identificador primitivo (`long`, `int`...) só é aceito pelo goal `repositories`; `services` e `resources`
  recusam a entidade com `Type parameter '...' is incorrect`. Use o tipo empacotado.
- Quando o tipo do identificador e `revision-number-class` têm o mesmo nome simples (por exemplo `Long` e
  `java.lang.Long`), a lista de parâmetros de tipo sai sem a vírgula entre eles (`<Cliente,LongLong>`) e o
  arquivo gerado não compila.
- Na tabela do log, a coluna de resultado está invertida: arquivo gerado com sucesso aparece como `Error #0`,
  e falha aparece como `Created`/`Overwritten`. Confira os arquivos em disco.
- `overwrite=true` apaga o arquivo existente, inclusive o que foi editado à mão.
- O módulo não tem testes.

## Referência

- Repositórios, serviços e controllers base: `archbase-domain-driven-design`
  (`br.com.archbase.ddd.infraestructure`)
- `@Logging`: [archbase-resource-logger](../archbase-resource-logger/README.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
