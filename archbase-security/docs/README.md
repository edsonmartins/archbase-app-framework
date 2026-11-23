# Documentação do módulo archbase-security — índice de `docs/`

Esta pasta guarda guias e a descrição das entidades. **As duas referências do módulo ficam fora
dela:**

| Documento | Conteúdo |
|---|---|
| [../ARQUITETURA.md](../ARQUITETURA.md) | **Modelo de autorização** — os cinco portões, qual anotação usar, por que negou. Comece aqui. |
| [../readme-security.md](../readme-security.md) | **Autenticação e customização** — login, sessões, logout, MFA, token de API, senha, cadeia de segurança própria |
| [../MODELO_CORE_AUTORIZACAO.md](../MODELO_CORE_AUTORIZACAO.md) | O desenho do core de autorização |
| [../../deployment/security-hardening.md](../../deployment/security-hardening.md) | O que ligar em produção e o que cada chave quebra |

---

## Guias — `guides/`

- [Configuração](guides/configuration.md) — propriedades e configuração do módulo
- [Anotações e uso](guides/annotations-and-usage.md) — `@HasPermission` e as demais anotações
- [Exemplos de código](guides/code-examples.md) — exemplos completos e casos de uso
- [Melhores práticas](guides/best-practices.md) — recomendações de desenho e segurança
- [Troubleshooting](guides/troubleshooting.md) — problemas comuns

## Entidades — `entities/`

- [Visão geral](entities/overview.md) — hierarquia das entidades e estratégia de herança
- [Entidades core](entities/core-entities.md) — usuário, grupo, perfil
- [Entidades de permissão](entities/permission-entities.md) — permissão, recurso, ação
- [Tokens](entities/token-entities.md) — access token, token de API, redefinição de senha

## Keycloak — `keycloak/` (pesquisa histórica)

[keycloak-archbase-integration-decisions.md](keycloak/keycloak-archbase-integration-decisions.md) e
`keycloak/research/` registram uma pesquisa anterior sobre integração com Keycloak. **As decisões
de lá não valem.** A integração com IdP externo foi implementada de outro jeito, no módulo
`archbase-security-oidc` — ver [../../archbase-security-oidc/README.md](../../archbase-security-oidc/README.md).

## Desatualizados — `architecture/` e `diagrams/`

> ⚠ **`architecture/*` e `diagrams/*` estão desatualizados e serão arquivados.** Descrevem o modelo
> anterior ao core de cinco portões, e trazem propriedades, rotas e classes que não existem mais (ou
> nunca existiram). Não os use como referência. Em vez deles:
>
> - autorização → [../ARQUITETURA.md](../ARQUITETURA.md)
> - autenticação, tokens, reset de senha → [../readme-security.md](../readme-security.md)

`archive/` guarda o documento original do qual esta pasta foi desmembrada.

---

## Início rápido

### 1. Dependência

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter</artifactId>
    <version>${archbase.version}</version>
</dependency>
```

`archbase-starter` traz core, segurança e multitenancy. `archbase-starter-core` sozinho **não**
traz o módulo de segurança: junte `archbase-starter-security` (ver
[../README.md](../README.md#como-ativar)).

### 2. Propriedades

```properties
# Obrigatórias, sem valor padrão
archbase.security.jwt.secret-key=${JWT_SECRET}          # Base64 padrão, >= 32 bytes decodificados
archbase.security.jwt.token-expiration=3600000          # ms
archbase.security.jwt.refresh-expiration=604800000      # ms
archbase.security.whitelist=
archbase.security.cors.allowed-origins=https://app.exemplo.com
archbase.security.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
archbase.security.cors.allowed-headers=Authorization,Content-Type,X-TENANT-ID,X-COMPANY-ID
archbase.security.cors.allow-credentials=true

# Onde estão os controllers anotados — alimenta o catálogo de capacidades
archbase.security.scan-packages=com.suaempresa

# Multitenancy (opcional)
archbase.multitenancy.enabled=true
```

`archbase.security.jwt.secret` e `archbase.security.jwt.expiration` não existem.

### 3. Proteger endpoints

```java
@RestController
@RequestMapping("/api/v1/users")
@ArchbaseResource(value = "app.usuario", description = "Usuários")
public class UsuarioController {

    @GetMapping("/{id}")
    @HasPermission(action = "view", description = "Consultar usuário")
    public UsuarioDto buscar(@PathVariable String id) { ... }

    @PostMapping
    @HasPermission(action = "create", description = "Criar usuário")
    public UsuarioDto criar(@RequestBody UsuarioDto usuario) { ... }
}
```

`description` é obrigatória — sem ela a anotação não compila. `@HasPermission` vai no método;
o recurso comum vai em `@ArchbaseResource`, na classe.

### 4. Catálogo e concessão

**Não crie `ResourceEntity` e `ActionEntity` à mão.** Na subida, o framework varre
`archbase.security.scan-packages` e registra no catálogo cada recurso e ação declarados nas
anotações, com a `description` delas (que na tabela é `NOT NULL`, tanto em `SEGURANCA_RECURSO`
quanto em `SEGURANCA_ACAO`).

A concessão é feita pelo admin — pela tela de permissões ou pela API:

```http
POST /api/v1/resource/permissions
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "securityId": "<id do usuário, grupo ou perfil>",
  "actionId":   "<id da ação no catálogo>",
  "type":       "USER"
}
```

`type` é `USER`, `GROUP` ou `PROFILE`. `effect` (`GRANT`, o padrão, ou `DENY`), `companyId` e
`projectId` são opcionais. O endpoint é administrativo: quem pode chamá-lo depende de
`archbase.security.admin-endpoints.policy`.
