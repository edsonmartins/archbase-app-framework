# Módulo archbase-security

Autenticação e autorização para aplicações Archbase: login com JWT (access e refresh persistidos),
MFA, token de API, reset de senha, e um core único de autorização que decide todo acesso em cinco
portões a partir de um catálogo de capacidades (recurso + ação) alimentado pelas próprias anotações
do código.

```
1 IDENTITY   2 SCOPE   3 RESTRICTION   4 LEVEL   5 GRANT
   nega        nega        nega          nega     CONCEDE
```

**Os portões 1 a 4 só sabem negar. Só o portão 5 concede.** `@RequireProfile` e companhia trancam;
`@HasPermission` abre. O modelo completo está em [ARQUITETURA.md](ARQUITETURA.md).

---

## Como ativar

O caminho normal é o starter completo, que traz core, segurança e multitenancy:

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter</artifactId>
    <version>${archbase.version}</version>
</dependency>
```

Ou, separadamente, `archbase-starter-core` + `archbase-starter-security`. Os dois papéis:

- `archbase-starter-core` varre o pacote `br.com.archbase.security` (componentes, entidades e
  repositórios);
- `archbase-starter-security` registra a cadeia padrão (`DefaultArchbaseSecurityConfiguration`), o
  `MethodSecurityConfig` das anotações e a rotina do esquema de segurança.

Usar o artefato `archbase-security` sem os starters exige que a aplicação faça esse registro por
conta própria.

---

## Propriedades mínimas

Todas obrigatórias, sem valor padrão — a aplicação não sobe sem elas:

```properties
# Base64 padrão, >= 32 bytes depois de decodificada (HS256)
archbase.security.jwt.secret-key=${JWT_SECRET}
# Milissegundos
archbase.security.jwt.token-expiration=3600000
archbase.security.jwt.refresh-expiration=604800000

# Lidas pela configuração padrão
archbase.security.whitelist=
archbase.security.cors.allowed-origins=https://app.exemplo.com
archbase.security.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
archbase.security.cors.allowed-headers=Authorization,Content-Type,X-TENANT-ID,X-COMPANY-ID
archbase.security.cors.allow-credentials=true
```

Não é obrigatória, mas sem ela o catálogo fica vazio e toda `@HasPermission` nega (`NO_GRANT`):

```properties
# Pacotes onde estão os controllers anotados — é daqui que o catálogo é alimentado
archbase.security.scan-packages=com.suaempresa
```

`archbase.security.jwt.secret` e `archbase.security.jwt.expiration` não existem. As demais chaves —
e as proteções que vêm desligadas por compatibilidade — estão em
[../deployment/security-hardening.md](../deployment/security-hardening.md).

---

## Protegendo um endpoint

```java
@RestController
@RequestMapping("/api/v1/ordens-servico")
@ArchbaseResource(value = "tms.ordemservico", description = "Ordem de serviço")
public class OrdemServicoController {

    @GetMapping
    @HasPermission(action = "view", description = "Listar ordens de serviço")
    public ResponseEntity<Page<OrdemServicoDto>> listar() { ... }

    @PostMapping
    @HasPermission(action = "create", description = "Criar ordem de serviço")
    public ResponseEntity<OrdemServicoDto> criar(@RequestBody OrdemServicoDto dto) { ... }
}
```

- `@HasPermission` é `@Target(METHOD)` — não compila na classe — e `description` é obrigatória.
- `resource` vazio herda o `value` do `@ArchbaseResource` da classe.
- Na subida, recurso e ação são registrados no catálogo a partir dessas anotações; o admin concede a
  capacidade a usuário, grupo ou perfil.
- `@PermitAll`, `@RolesAllowed` e `@Secured` são ignorados. Endpoint público vai em
  `archbase.security.whitelist`.

---

## Documentos

| Documento | Conteúdo |
|---|---|
| [ARQUITETURA.md](ARQUITETURA.md) | **Modelo de autorização** — os cinco portões, qual anotação usar, por que negou, diagnóstico. Comece aqui. |
| [readme-security.md](readme-security.md) | **Autenticação e customização** — login, sessões, logout, MFA, token de API, senha, tenant no login, ganchos de negócio, cadeia de segurança própria |
| [MODELO_CORE_AUTORIZACAO.md](MODELO_CORE_AUTORIZACAO.md) | O desenho do core de autorização e por que ele é assim |
| [CONTRATO_DEPENDENCIAS_DE_CAPACIDADE.md](CONTRATO_DEPENDENCIAS_DE_CAPACIDADE.md) | Dependências entre capacidades (implementado) |
| [docs/guides/](docs/guides/) | Guias de configuração, anotações, exemplos, boas práticas e troubleshooting |
| [docs/README.md](docs/README.md) | Índice da pasta `docs/` |
| [../deployment/security-hardening.md](../deployment/security-hardening.md) | O que ligar em produção, em que ordem, e o que cada chave quebra |
| [../deployment/esquema-de-seguranca.md](../deployment/esquema-de-seguranca.md) | As tabelas de segurança que o framework cria na subida |
| [../archbase-security-oidc/README.md](../archbase-security-oidc/README.md) | Login por IdP OIDC externo, ao lado do login próprio |
