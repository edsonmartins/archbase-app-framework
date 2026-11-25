# archbase-test-utils

Utilitários para testes: um construtor de entidades por reflexão (`EntityBuilder`), um registro de fixtures
(`EntityFixture`), uma anotação de usuário autenticado simulado (`@WithMockArchbaseUser`) e uma anotação
composta de teste de integração (`@ArchbaseTest`).

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-test-utils</artifactId>
    <version>3.5.0</version>
    <scope>test</scope>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

Todas as dependências são de escopo `compile` no módulo, por isso o `<scope>test</scope>` acima:

| Dependência | Observação |
|---|---|
| `spring-boot-starter-test` | Sem `junit-vintage-engine` |
| `spring-security-test` | Base de `@WithMockArchbaseUser` |
| `archbase-domain-driven-design-spec` | |
| `lombok`, `jakarta.persistence-api` | |
| `archbase-multitenancy`, `archbase-security` | `optional`: não chegam ao projeto, e nenhuma classe do módulo as usa |

## Auto-configuração

Não tem. São classes e anotações usadas diretamente nos testes.

## `EntityBuilder`

Cria a instância pelo construtor sem argumentos e atribui campos por reflexão (procura o campo na classe e nas
superclasses, ignorando visibilidade). Campo inexistente lança `EntityBuilder.EntityBuilderException`.

```java
Cliente cliente = EntityBuilder.of(Cliente.class)
        .set("nome", "Maria")
        .set("ativo", true)
        .with(c -> c.adicionarTelefone("11 99999-0000"))   // roda depois dos set(...)
        .build();

// Sem construtor sem argumentos: informe a fábrica
Cliente outro = EntityBuilder.of(Cliente.class)
        .set("nome", "João")
        .build(() -> new Cliente("COD-1"));
```

`EntityBuilder.withDefaults(Classe.class)` pré-preenche `id`, `code` (`"DEFAULT-<timestamp>"`), `version` (`0L`) e
`active` (`true`), cada um só quando o campo existe e é do tipo certo. O `id` é um UUID novo: como texto se o campo
for `String` (o caso de `PersistenceEntityBase`), como `java.util.UUID` se for desse tipo. Até a 3.5.0 o `id` era
sempre `UUID`, e `build()` falhava em toda entidade com id `String`.

## `EntityFixture`

Registro estático (um `HashMap` global, não sincronizado) de fábricas por nome:

```java
EntityFixture.register("cliente-valido", () -> EntityBuilder.of(Cliente.class)
        .set("nome", "Cliente Teste")
        .build());

Cliente c = EntityFixture.create("cliente-valido");   // nova instância a cada chamada
```

Também há `unregister(nome)`, `clear()` e `isRegistered(nome)`. Nome não registrado lança
`IllegalArgumentException`. O registro sobrevive entre classes de teste da mesma JVM; chame `clear()` em
`@AfterEach`/`@AfterAll` se isso importar.

## `@WithMockArchbaseUser`

Anotação de `spring-security-test` (`@WithSecurityContext`) para método ou classe. Monta um
`UsernamePasswordAuthenticationToken` com:

- principal `WithMockArchbaseUserFactory.ArchbaseTestPrincipal(username, tenantId, companyId, projectId)`;
- credencial `password`;
- uma autoridade por item de `roles`, com o prefixo `ROLE_` acrescentado quando falta.

| Atributo | Padrão |
|---|---|
| `username` | `test@example.com` |
| `password` | `password` |
| `roles` | `{"USER"}` |
| `tenantId` | `test-tenant` |
| `companyId` | `""` |
| `projectId` | `""` |

```java
@Test
@WithMockArchbaseUser(username = "admin@acme.com", roles = {"ADMIN"})
void listaClientes() {
    // SecurityContextHolder tem um usuário autenticado com ROLE_ADMIN
}
```

O que a anotação **não** faz:

- não preenche `ArchbaseTenantContext`: `tenantId`, `companyId` e `projectId` ficam só dentro do principal;
- o principal não é o `UserEntity` do `archbase-security`. Tudo que exige esse tipo trata o usuário como ausente:
  `ArchbaseCurrentUser.of(...)` devolve vazio e a avaliação de `@HasPermission` nega o acesso. Serve para
  `@PreAuthorize("hasRole(...)")` e código que só olha as autoridades;

O principal (`ArchbaseTestPrincipal`) implementa `java.security.Principal`, então `authentication.getName()`
devolve o `username`. Até a 3.5.0 devolvia o `toString()` do record.

## `@ArchbaseTest`

Combina `@SpringBootTest` e `@ActiveProfiles("test")`, com aliases `classes`, `properties`, `value` e `profiles`.

Os perfis padrão são `test`; `value` e `profiles` são o mesmo atributo:

```java
@ArchbaseTest(classes = MinhaAplicacao.class, properties = "archbase.security.schema.mode=off")
class ClienteRepositoryTest {
}

@ArchbaseTest(value = "integracao", classes = MinhaAplicacao.class)
class ClienteIntegracaoTest {
}
```

Até a 3.5.0 a anotação não podia ser usada: `value` e `profiles` tinham padrões diferentes (`{}` e `"test"`) e o
Spring recusava a anotação (`AnnotationConfigurationException: Misconfigured aliases`).

## Referência

- Segurança (`UserEntity`, `@HasPermission`): [archbase-security](../archbase-security/README.md)
- Tenant: [archbase-multitenancy](../archbase-multitenancy/README.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
