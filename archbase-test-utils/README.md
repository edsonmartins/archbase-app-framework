# archbase-test-utils

Utilitários para testes: um construtor de entidades por reflexão (`EntityBuilder`), um registro de fixtures
(`EntityFixture`), uma anotação de usuário autenticado simulado (`@WithMockArchbaseUser`) e uma anotação
composta de teste de integração (`@ArchbaseTest`, inutilizável na 3.5.0 — ver abaixo).

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

`EntityBuilder.withDefaults(Classe.class)` pré-preenche, quando o campo existe: `id` com um `java.util.UUID`,
`code` com `"DEFAULT-<timestamp>"`, `version` com `0L` e `active` com `true`. Em entidades que estendem
`PersistenceEntityBase` o `id` é `String`, e `build()` falha com `EntityBuilderException` (causa:
`IllegalArgumentException` ao atribuir `UUID` a `String`). Nelas, use `of(...)`: o construtor de
`PersistenceEntityBase` já gera o `id`.

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
- `authentication.getName()` não devolve o `username`: como o principal não implementa `Principal` nem
  `UserDetails`, o Spring usa o `toString()` do record
  (`ArchbaseTestPrincipal[username=test@example.com, tenantId=test-tenant, ...]`). Para ler o nome, faça o cast
  do principal para `ArchbaseTestPrincipal` e use `username()`.

## `@ArchbaseTest`

Combina `@SpringBootTest` e `@ActiveProfiles("test")`, com aliases `classes`, `properties`, `value` e `profiles`.

**Não funciona na 3.5.0.** `value` e `profiles` são aliases do mesmo atributo de `@ActiveProfiles` com padrões
diferentes (`{}` e `"test"`), e o Spring recusa a anotação ao processá-la:
`AnnotationConfigurationException: Misconfigured aliases: attribute 'profiles' ... and attribute 'value' ... must
declare the same default value`. Qualquer classe anotada com `@ArchbaseTest` falha ao montar o contexto de
teste, com ou sem atributos. Use diretamente:

```java
@SpringBootTest
@ActiveProfiles("test")
class ClienteRepositoryTest {
}
```

## Referência

- Segurança (`UserEntity`, `@HasPermission`): [archbase-security](../archbase-security/README.md)
- Tenant: [archbase-multitenancy](../archbase-multitenancy/README.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
