# archbase-architecture-rules

Regras de arquitetura prontas, sobre [Taikai](https://github.com/enofex/taikai) 1.63.0 e ArchUnit 1.4.2, para
rodar em testes JUnit 5 da aplicação: convenções DDD e Spring do Archbase, nomenclatura, segurança, multitenancy,
modelagem de agregados e os estilos declarados com [`archbase-architecture`](../archbase-architecture/README.md).

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-architecture-rules</artifactId>
    <version>3.5.0</version>
    <scope>test</scope>
</dependency>
```

Traz Taikai, `archunit` e `archunit-junit5` 1.4.2, `archbase-domain-driven-design-spec`, `archbase-architecture`,
`spring-web`, `jakarta.persistence-api` e `junit-jupiter-api`. `archbase-security`, `archbase-multitenancy` e
`archbase-shared-kernel` são opcionais (não vêm junto).

## Auto-configuração

Não tem. É biblioteca de teste.

## Uso

```java
class ArquiteturaTest {

    @Test
    void segueOsPadroes() {
        ArchbaseArchitectureRules.forNamespace("com.acme.app")
            .withDddRules()
            .withSpringRules()
            .withArchitectureStyleRules(ArchbaseArchitectureRules.ArchitectureStyle.LAYERED)
            .checkAll(true)
            .check();
    }
}
```

`check()` monta um `Taikai` com `namespace` e `failOnEmpty` e chama `check()` (para no primeiro erro) ou, com
`checkAll(true)`, `checkAll()`. `importClasses()` devolve as `JavaClasses` do namespace (sem testes) para regras
próprias; `addRule(TaikaiRule)` acrescenta uma regra.

### O que cada grupo liga

Sempre, em qualquer `check()`: as regras Java do Taikai `noUsageOfDeprecatedAPIs`,
`classesShouldImplementHashCodeAndEquals` e `imports.shouldHaveNoCycles`.

| Método | Regras |
|---|---|
| `withDddRules()` | `@Entity` deve ser atribuível a `PersistenceEntityBase`; interface `*Repository` deve estender o `Repository` do Archbase ou do Spring Data; repositório sem métodos derivados (`findBy*`, `countBy*`, `...OrderBy...` etc.) nem `@Query`; nada em `..domain..` depende de `..infrastructure..`; `@Entity` em `..domain..`, `..entity..`, `..persistence..` ou `..model..` |
| `withSpringRules()` | Taikai: sem `@Autowired` em campo; controllers com `@RestController`, nome `*Controller`, sem depender de outro controller; services `*Service`, sem depender de controller; repositories `*Repository`, sem depender de service; configurações `*Configuration`. Mais: controller não depende de `@Repository`; controller só depende de `java..`, `jakarta..`, `org.springframework..`, `org.slf4j..`, `io.swagger..`, `..dto..`, `..service..`, `..exception..`, `br.com.archbase..` |
| `withNamingRules()` | Taikai: interface sem prefixo `I`, constantes, pacotes. Mais: nada termina em `Impl`; classe em `..dto..` termina em `DTO`/`Dto`; exceção termina em `Exception`; classe em `..test..`/`..tests..` termina em `Test`, `Tests` ou `IT` |
| `withSecurityRules()` | Método público de `@RestController` com `@HasPermission`, `@PermitAll` ou `@PreAuthorize`; classe `@RestController` com `@Secured`, `@HasPermission` ou `@RolesAllowed`, ou nome contendo `Public` (ver limitações) |
| `withMultitenancyRules()` | `@Entity` deve ser atribuível a `TenantPersistenceEntityBase` |
| `withTestRules()` | Taikai JUnit 5: classe sem `@Disabled`; método de teste com asserção ou verificação |
| `withAllRules()` | Os seis acima. **Não** inclui agregados nem estilos |
| `withAggregateRules()` | `ArchbaseAggregateRules`: entidade (`Entity<A, ID>`) só aparece em campo do agregado `A`; tipo de domínio não guarda referência direta a raiz de agregado (`AggregateRoot` ou `@DomainAggregateRoot`), só id ou associação. Coleções, `Optional`, valores de `Map` e arrays são desembrulhados |
| `withArchitectureStyleRules(ArchitectureStyle...)` | `ArchbaseArchitectureStyleRules`: `LAYERED`, `LAYERED_STRICT`, `ONION_CLASSICAL`, `ONION_SIMPLIFIED`, `HEXAGONAL` |

Regras de estilo: o tipo pega a anotação mais próxima (o próprio tipo, depois o pacote e os pacotes pais); tipo sem
anotação na cadeia é ignorado, então ligar a regra num projeto sem as anotações não acusa nada. Camadas: só
dependência para a mesma camada ou para baixo (`LAYERED_STRICT`: só a imediatamente abaixo). Onion: anel interno não
depende de externo. Hexagonal: aplicação e portas não dependem de adaptadores; adaptador primário e secundário não
dependem um do outro (`@Adapter` genérico fica isento disso).

`ArchbaseDddRules`, `ArchbaseSpringRules`, `ArchbaseNamingRules`, `ArchbaseAggregateRules` e
`ArchbaseArchitectureStyleRules` expõem cada regra como método estático que devolve `ArchRule`, inclusive algumas
que nenhum grupo liga (`aggregatesShouldNotReferenceOtherAggregatesDirectly`, `controllersShouldBePackagePrivate`,
`enumsShouldResideInCorrectPackages`, `mappersShouldEndWithMapper`, `adaptersShouldEndWithAdapter`,
`portsShouldEndWithPort`, `valueObjectsShouldResideInCorrectPackages` etc.).

### Perfis (`ArchbaseRuleProfiles`)

Todos com `checkAll(true)`.

| Perfil | Grupos |
|---|---|
| `multitenantRestApi(ns)` | DDD, Spring, nomenclatura, segurança, multitenancy |
| `simpleRestApi(ns)` | DDD, Spring, nomenclatura, segurança |
| `simpleService(ns)` | DDD, Spring, nomenclatura |
| `domainOnly(ns)` | DDD, nomenclatura |
| `strict(ns)` | `withAllRules()` |
| `lenient(ns)` | nomenclatura, com `failOnEmpty(false)` |

### Classe base

`br.com.archbase.architecture.rules.test.ArchbaseArchitectureTest` é abstrata e já tem o `@Test`. Implemente
`getBasePackage()`; DDD, Spring e nomenclatura vêm ligados, segurança, multitenancy e testes desligados
(`enable...Rules()`), `failOnEmpty()` e `checkAll()` `true`; `configureCustomRules(rules)` é o gancho para o resto.

```java
class MinhaArquiteturaTest extends ArchbaseArchitectureTest {
    @Override
    protected String getBasePackage() {
        return "com.acme.app";
    }

    @Override
    protected void configureCustomRules(ArchbaseArchitectureRules rules) {
        rules.withAggregateRules();
    }
}
```

## Limitações

- **Versão do ArchUnit.** O pom raiz gerencia `archunit` em 1.4.2 (`archunit.version`). Até a 3.5.0 gerenciava 1.0.1,
  que não lê bytecode Java 25 (importa zero classes), e o [`archbase-modular-monolith`](../archbase-modular-monolith/README.md)
  declarava 1.0.1: com os dois no projeto podia vir 1.0.1, e as regras passavam sem examinar nada. Se o seu projeto
  fixa outra versão, confira com `mvn dependency:tree -Dincludes=com.tngtech.archunit`.
- **Regras de segurança desalinhadas com o Archbase.** `@HasPermission` só vale em método
  (`@Target(METHOD)`), então a regra de classe só passa com `@Secured`/`@RolesAllowed` na classe ou `Public` no
  nome, e o Archbase ignora `@Secured`, `@RolesAllowed` e `@PermitAll` (só `prePostEnabled` está ligado). Um
  controller que passa nessas regras pode não estar protegido por anotação nenhuma.
- **Entidades por empresa.** `CompanyPersistenceEntityBase` não estende `PersistenceEntityBase` nem
  `TenantPersistenceEntityBase`; entidades que a estendem violam as regras DDD e de multitenancy.
- `withAggregateRules()` acusa cada `@ManyToOne` para outro agregado em modelos em que a entidade JPA é o próprio
  agregado. Por isso fica fora de `withAllRules()`.
- `ExampleArchitectureTest` (pacote `examples`) está em `src/main` com `@Disabled`; serve só de leitura.

## Referência

- [`archbase-architecture`](../archbase-architecture/README.md): as anotações de estilo.
- [`archbase-validation-ddd-model`](../archbase-validation-ddd-model/README.md): predicados ArchUnit antigos para o modelo DDD.
