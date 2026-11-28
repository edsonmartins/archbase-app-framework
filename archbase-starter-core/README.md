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
| `ArchbaseServerMvcConfiguration` | `archbase.web.mvc.enabled` (padrão `true`) e ausência de bean chamado `customWebMvcConfigurer` | `@EnableTransactionManagement`, `@EnableJpaAuditing(auditorAwareRef = "auditorAware")`, `MessageSource` (`classpath:messages`, UTF-8), `LocaleChangeInterceptor` (parâmetro `lang`), aspecto `SimpleArchbaseResourceAspect`; `auditorAware` que não atribui autor quando o `archbase-security` está ausente ou desligado (e a aplicação não declarou o seu); importa a varredura abaixo |
| `ArchbaseComponentScanConfiguration` | `archbase.web.mvc.enabled` (padrão `true`) | Varredura de pacotes do framework; `@EntityScan` do pacote de entidades de segurança; pacotes da aplicação por propriedade |
| `ArchbaseThreadConfiguration` | sem bean `TaskExecutor` | `threadPoolTaskExecutor`: 6 a 12 threads, daemon, prefixo `archbase-task-executor`; aplica o `TaskDecorator` do contexto, se houver um só |
| `ArchbaseRSQLConfiguration` | `archbase.rsql.enabled` (padrão `true`) | `ArchbaseRSQLJPASupport` com os `EntityManager` do contexto |
| `ArchbaseBeanValidateConfiguration` | sem bean `jakarta.validation.Validator` | `Validator` do Hibernate Validator com `ArchbaseMessageInterpolator` |

Propriedades lidas por este starter:

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.web.mvc.enabled` | `true` | `false` desliga a configuração MVC **e** a varredura dos pacotes do framework |
| `archbase.rsql.enabled` | `true` | Registra o `ArchbaseRSQLJPASupport` |
| `archbase.rsql.max-page-size` | `1000` | Tamanho máximo de página nas consultas; `<= 0` desliga o limite |
| `archbase.rsql.strict-path-access-control` | `false` | A lista branca do RSQL vale também para os segmentos intermediários do seletor (ver [archbase-query](../archbase-query/README.md)) |
| `archbase.app.component.scan` | — | Pacotes extras para component scan, separados por vírgula |
| `archbase.app.jpa.entities` | — | Pacotes extras de entidades (equivale a `@EntityScan`) |
| `archbase.app.jpa.repositories` | — | Pacotes extras de repositórios; cada interface que estende `Repository` vira um repositório com base `CommonArchbaseJpaRepository` |

As três últimas só têm efeito com `archbase.web.mvc.enabled=true`, porque são processadas pela varredura.

## A varredura de pacotes

`ArchbaseComponentScanConfiguration` faz component scan de:

- `br.com.archbase.ddd.infraestructure.aspect`
- `br.com.archbase.multitenancy`

e registra `br.com.archbase.security.persistence` como pacote de entidades.

**Segurança não entra por aqui** desde a 3.5.0: quem registra os componentes do `archbase-security` é o
[`archbase-starter-security`](../archbase-starter-security/README.md), respeitando `archbase.security.enabled`. Fica
um caminho de compatibilidade para quem usa este starter com o jar `archbase-security` **sem** o starter de
segurança: a varredura de `br.com.archbase.security` e os repositórios continuam aqui, agora também condicionados a
`archbase.security.enabled` (e, como antes, a `archbase.web.mvc.enabled`).

### Multitenancy já vem pela varredura

Como `archbase-multitenancy` é dependência deste starter e `br.com.archbase.multitenancy` está na varredura, o
starter-core sozinho já registra o resolvedor de tenant do Hibernate (`ArchbaseCurrentTenantIdentifierResolverImpl`),
o `TenantProperties` (`archbase.app.tenant.default.id`) e o aspecto de empresa. O interceptor que
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
- Segurança: [archbase-starter-security](../archbase-starter-security/README.md)
- Tenant: [archbase-multitenancy](../archbase-multitenancy/README.md)
