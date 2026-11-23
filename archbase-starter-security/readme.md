# archbase-starter-security

Liga o `archbase-security`: autenticação JWT, cadeia de filtros, segurança por método, cifragem de segredos e a
rotina que cria as tabelas de segurança.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-security</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## Não use sozinho

Este starter depende só de `archbase-security`. Os serviços, filtros e avaliadores do módulo
(`br.com.archbase.security.*`) entram no contexto pela varredura do **`archbase-starter-core`**
(ver [readme](../archbase-starter-core/readme.md#a-varredura-de-pacotes)), que este starter não traz. Sem ela, a
`DefaultArchbaseSecurityConfiguration` importada aqui pede o `ArchbaseJwtAuthenticationFilter`, que é `@Component`
e não foi registrado.

Use o [`archbase-starter`](../archbase-starter/readme.md), ou declare `archbase-starter-core` junto, e não ponha
`archbase.web.mvc.enabled=false`. A alternativa é a aplicação fazer por conta própria o que o starter-core faz: varrer
`br.com.archbase.security` e registrar as entidades de `br.com.archbase.security.persistence` e os repositórios de
`br.com.archbase.security.repository`.

## Auto-configuração

| Classe | Condição | O que faz |
|---|---|---|
| `ArchbaseSecurityAutoConfiguration` | `SecurityFilterChain` no classpath e `archbase.security.enabled` (padrão `true`) | Importa `ArchbaseSecurityApplicationConfig`, `DefaultArchbaseSecurityConfiguration` (cadeia padrão; não é aplicada se houver bean `CustomSecurityConfiguration`) e `MethodSecurityConfig` (`prePostEnabled`) |
| `ArchbaseSecuritySchemaAutoConfiguration` | `DataSource` no classpath e `archbase.security.enabled` (padrão `true`) | Importa `ArchbaseSecuritySchemaConfiguration`: compara as entidades de segurança com o banco na subida e cria o que falta. Um bean `ArchbaseSecuritySchemaInitializer` da aplicação substitui o padrão |
| `ArchbaseCryptoAutoConfiguration` | nenhuma — sempre registrada | `ArchbaseCryptoService` (AES-GCM, chave em `archbase.security.crypto.key`), `ArchbaseEncryptedStringConverter`, `ArchbaseColumnReencryptor`; cada um substituível por bean próprio |

### `archbase.security.enabled=false` não desliga tudo

A propriedade só controla as duas primeiras auto-configurações. Continuam ativos:

- a cripto (`ArchbaseCryptoAutoConfiguration` não tem condição);
- tudo o que a varredura do `archbase-starter-core` encontra em `br.com.archbase.security`, inclusive as classes
  `@Configuration` do módulo — a varredura não consulta essa propriedade.

Com o `archbase-starter`, portanto, `enabled=false` não deixa a aplicação sem segurança configurada nem dispensa as
propriedades obrigatórias.

## Propriedades obrigatórias

Sem valor padrão; sem qualquer uma, o placeholder não resolve e a aplicação não sobe.

| Propriedade | Lida por |
|---|---|
| `archbase.security.jwt.secret-key` | `ArchbaseJwtService`. Base64 **padrão** (não URL-safe), no mínimo 32 bytes depois de decodificada |
| `archbase.security.jwt.token-expiration` | `ArchbaseJwtService`, em milissegundos |
| `archbase.security.jwt.refresh-expiration` | `ArchbaseJwtService`, em milissegundos |
| `archbase.security.whitelist` | `DefaultArchbaseSecurityConfiguration`. Vazio é válido |
| `archbase.security.cors.allowed-origins` | `DefaultArchbaseSecurityConfiguration` |
| `archbase.security.cors.allowed-methods` | `DefaultArchbaseSecurityConfiguration` |
| `archbase.security.cors.allowed-headers` | `DefaultArchbaseSecurityConfiguration` |
| `archbase.security.cors.allow-credentials` | `DefaultArchbaseSecurityConfiguration` |

```properties
archbase.security.jwt.secret-key=${JWT_SECRET}        # openssl rand -base64 48
archbase.security.jwt.token-expiration=3600000
archbase.security.jwt.refresh-expiration=604800000
archbase.security.whitelist=
archbase.security.cors.allowed-origins=https://app.exemplo.com
archbase.security.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
archbase.security.cors.allowed-headers=*
archbase.security.cors.allow-credentials=true
```

Uma aplicação com cadeia própria (`CustomSecurityConfiguration`) não usa a whitelist nem o CORS, mas as chaves de
JWT continuam obrigatórias.

Sem obrigatoriedade, mas sem efeito se vazias: `archbase.security.crypto.key` (ativar MFA falha) e
`archbase.security.scan-packages` (nenhum `@HasPermission` é sincronizado).

## Padrões que preservam comportamento antigo

Várias proteções vêm desligadas para que atualizar o framework não quebre quem está em produção. Antes de ir para
produção, leia o [guia de endurecimento](../deployment/security-hardening.md) e o checklist do
[guia de configuração](../archbase-security/docs/guides/configuration.md).

## Referência

| Tema | Onde |
|---|---|
| Todas as propriedades, com padrão | [archbase-security/docs/guides/configuration.md](../archbase-security/docs/guides/configuration.md) |
| Login, sessões, MFA, token de API, customização da cadeia | [archbase-security/readme-security.md](../archbase-security/readme-security.md) |
| Autorização (cinco portões, anotações) | [archbase-security/ARQUITETURA.md](../archbase-security/ARQUITETURA.md) |
| Tabelas criadas na subida | [deployment/esquema-de-seguranca.md](../deployment/esquema-de-seguranca.md) |
| IdP externo (opt-in) | [archbase-security-oidc/README.md](../archbase-security-oidc/README.md) |
| Control Plane (opt-in) | [archbase-controlplane/README.md](../archbase-controlplane/README.md) |
| Visão geral do framework | [CLAUDE.md](../CLAUDE.md) |
