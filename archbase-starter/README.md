# archbase-starter

Agregador. Não tem código próprio além de uma classe de configuração: só junta os três starters de base.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: o pom raiz herda do `spring-boot-starter-parent` e cada artefato `br.com.archbase` é
declarado com a versão explícita. Use a **mesma versão** em todos os módulos Archbase da aplicação.

## O que traz

| Dependência | Doc |
|---|---|
| `archbase-starter-core` | [readme](../archbase-starter-core/README.md) |
| `archbase-starter-security` | [readme](../archbase-starter-security/README.md) |
| `archbase-starter-multitenancy` | [readme](../archbase-starter-multitenancy/README.md) |

É a combinação recomendada para aplicação com login e tenant. Cada starter registra o que é seu e também funciona
sozinho; este agregador junta os três e acrescenta o que só o core tem (registro dos repositórios da aplicação
por propriedade, configuração MVC, auditoria JPA).

## O que liga por padrão

O `ArchbaseStarterAutoConfiguration` importa as auto-configurações dos três starters, que também são
registradas por cada um deles. Na prática, com este starter e nenhuma propriedade de liga/desliga:

| Recurso | Propriedade | Padrão |
|---|---|---|
| Configuração MVC, varredura de pacotes do framework, auditoria JPA | `archbase.web.mvc.enabled` | `true` |
| Suporte RSQL | `archbase.rsql.enabled` | `true` |
| Segurança (JWT, cadeia de filtros, segurança por método, esquema de segurança) | `archbase.security.enabled` | `true` |
| Interceptor de tenant e propagação de contexto para `@Async` | `archbase.multitenancy.enabled` | `true` |

Detalhes, e o que **não** é desligado por essas propriedades, nos readmes de cada starter.

## Propriedades obrigatórias

As do `archbase-security`, que não têm valor padrão — sem elas a aplicação não sobe:

```properties
archbase.security.jwt.secret-key=${JWT_SECRET}        # Base64 padrão, >= 32 bytes decodificados
archbase.security.jwt.token-expiration=3600000        # ms
archbase.security.jwt.refresh-expiration=604800000    # ms
archbase.security.whitelist=                          # vazio é válido
archbase.security.cors.allowed-origins=https://app.exemplo.com
archbase.security.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
archbase.security.cors.allowed-headers=*
archbase.security.cors.allow-credentials=true
```

Recomendado configurar também `archbase.app.tenant.default.id` (ver
[archbase-multitenancy](../archbase-multitenancy/README.md#sem-tenant-no-contexto)).

## O que não vem

Módulos opt-in ficam fora deste starter e só são carregados se a aplicação adicionar a dependência:
`archbase-security-oidc` ([README](../archbase-security-oidc/README.md)), `archbase-controlplane`
([README](../archbase-controlplane/README.md)), hypersistence, flyway, analytics, offline-sync, mensageria
(starters próprios). Lista completa de módulos no [CLAUDE.md](../CLAUDE.md).

## Referência

- Propriedades de segurança e tenant: [archbase-security/docs/guides/configuration.md](../archbase-security/docs/guides/configuration.md)
- Autenticação: [archbase-security/readme-security.md](../archbase-security/readme-security.md)
- Autorização: [archbase-security/ARQUITETURA.md](../archbase-security/ARQUITETURA.md)
- Ligar as proteções em produção: [deployment/security-hardening.md](../deployment/security-hardening.md)
