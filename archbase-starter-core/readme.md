# archbase-starter-core

Base de persistência e MVC do Archbase: repositórios JPA, RSQL, validação, auditoria JPA e a varredura dos pacotes
do framework.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-core</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

Dependências de compilação do `pom.xml`:

| Dependência | Para quê |
|---|---|
| `archbase-domain-driven-design` | Repositórios JPA (`ArchbaseCommonJpaRepository`), infraestrutura DDD; traz `archbase-query` (RSQL) e `archbase-error-handling` |
| `archbase-domain-driven-design-spec` | Contratos DDD, bases de entidade, `ArchbaseTenantContext` |
| `archbase-multitenancy` | Resolvedor de tenant do Hibernate e aspecto de empresa — ver [abaixo](#multitenancy-já-vem-pela-varredura) |
| `archbase-resource-logger` | Aspecto de log de controller (`@Logging` / `@NoLogging`) |
| `archbase-validation` | Validação e `ArchbaseMessageInterpolator` |
| `spring-boot-persistence` | `@EntityScan` e `EntityScanPackages` |
| `modelmapper` | Só a biblioteca; este starter não registra bean de `ModelMapper` |
| `jackson-datatype-hibernate7` (`tools.jackson.datatype`) | Só a biblioteca; este starter não registra o módulo no Jackson |

Não traz `archbase-security`.

## Auto-configuração

Registrada: `ArchbaseCoreAutoConfiguration`, que importa:

| Configuração | Condição | O que registra |
|---|---|---|
| `ArchbaseServerMvcConfiguration` | `archbase.web.mvc.enabled` (padrão `true`) e ausência de bean chamado `customWebMvcConfigurer` | `@EnableTransactionManagement`, `@EnableJpaAuditing(auditorAwareRef = "auditorAware")`, `MessageSource` (`classpath:messages`, UTF-8), `LocaleChangeInterceptor` (parâmetro `lang`), aspecto `SimpleArchbaseResourceAspect`; importa a varredura abaixo |
| `ArchbaseComponentScanConfiguration` | `archbase.web.mvc.enabled` (padrão `true`) | Varredura de pacotes do framework; `@EntityScan` e `@EnableJpaRepositories` dos pacotes de segurança; pacotes da aplicação por propriedade |
| `ArchbaseThreadConfiguration` | sem bean `TaskExecutor` | `threadPoolTaskExecutor`: 6 a 12 threads, daemon, prefixo `archbase-task-executor` |
| `ArchbaseRSQLConfiguration` | `archbase.rsql.enabled` (padrão `true`) | `ArchbaseRSQLJPASupport` com os `EntityManager` do contexto |
| `ArchbaseBeanValidateConfiguration` | sem bean `jakarta.validation.Validator` | `Validator` do Hibernate Validator com `ArchbaseMessageInterpolator` |

Propriedades lidas por este starter:

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.web.mvc.enabled` | `true` | `false` desliga a configuração MVC **e** a varredura dos pacotes do framework |
| `archbase.rsql.enabled` | `true` | Registra o `ArchbaseRSQLJPASupport` |
| `archbase.rsql.max-page-size` | `1000` | Tamanho máximo de página nas consultas; `<= 0` desliga o limite |
| `archbase.app.component.scan` | — | Pacotes extras para component scan, separados por vírgula |
| `archbase.app.jpa.entities` | — | Pacotes extras de entidades (equivale a `@EntityScan`) |
| `archbase.app.jpa.repositories` | — | Pacotes extras de repositórios; cada interface que estende `Repository` vira um repositório com base `CommonArchbaseJpaRepository` |

As três últimas só têm efeito com `archbase.web.mvc.enabled=true`, porque são processadas pela varredura.

## A varredura de pacotes

`ArchbaseComponentScanConfiguration` faz component scan de:

- `br.com.archbase.ddd.infraestructure.aspect`
- `br.com.archbase.multitenancy`
- `br.com.archbase.security`
- `br.com.archbase.web.config`

e registra `br.com.archbase.security.persistence` como pacote de entidades e `br.com.archbase.security.repository`
como pacote de repositórios.

Consequências que valem saber:

- **É por aqui que os beans do `archbase-security` entram no contexto.** O `archbase-starter-security` sozinho não
  os registra. Ver [archbase-starter-security](../archbase-starter-security/readme.md).
- **A varredura não obedece `archbase.security.enabled` nem `archbase.multitenancy.enabled`.** Com o jar
  `archbase-security` no classpath, as classes `@Configuration`/`@Component` de `br.com.archbase.security` são
  registradas enquanto `archbase.web.mvc.enabled` for `true`.
- **Auditoria JPA precisa de um bean `auditorAware`.** Ele é declarado pelo `archbase-security`
  (`ArchbaseSecurityApplicationConfig`). Usando este starter sem o security, a aplicação declara o próprio
  `AuditorAware<String>` com esse nome.

### Multitenancy já vem pela varredura

Como `archbase-multitenancy` é dependência deste starter e `br.com.archbase.multitenancy` está na varredura, o
starter-core sozinho já registra o resolvedor de tenant do Hibernate (`ArchbaseCurrentTenantIdentifierResolverImpl`),
o `TenantProperties` (`archbase.app.tenant.default.id`), o aspecto de empresa e o `AsyncConfig`. O interceptor que
lê `X-TENANT-ID` não vem: ele é do `archbase-starter-multitenancy`. Detalhes em
[archbase-multitenancy](../archbase-multitenancy/README.md).

## Substituir o que o starter registra

| Para trocar | Declare |
|---|---|
| Configuração MVC inteira (e a varredura) | um bean chamado `customWebMvcConfigurer` |
| Executor de tarefas | um bean `TaskExecutor` |
| Validator | um bean `jakarta.validation.Validator` |

## Referência

- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
- Segurança: [archbase-starter-security](../archbase-starter-security/readme.md)
- Tenant: [archbase-multitenancy](../archbase-multitenancy/README.md)
