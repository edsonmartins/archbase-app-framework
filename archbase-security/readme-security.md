# Segurança do Archbase — autenticação e customização

Este é o documento de referência de **autenticação** do `archbase-security`: login, sessões
(access e refresh), logout, MFA, token de API, senha, tenant no login, ganchos de regra de negócio e
customização da cadeia de segurança.

**Autorização** — os cinco portões, qual anotação usar, por que negou — está em
[ARQUITETURA.md](ARQUITETURA.md). A seção [Anotações](#anotações-de-segurança-do-archbase) abaixo é o
resumo prático.

| Tema | Onde |
|---|---|
| Modelo de autorização | [ARQUITETURA.md](ARQUITETURA.md) |
| Desenho do core de autorização | [MODELO_CORE_AUTORIZACAO.md](MODELO_CORE_AUTORIZACAO.md) |
| Ligar as proteções sem quebrar produção | [../deployment/security-hardening.md](../deployment/security-hardening.md) |
| Tabelas de segurança criadas na subida | [../deployment/esquema-de-seguranca.md](../deployment/esquema-de-seguranca.md) |
| Login por IdP externo (OIDC) | [../archbase-security-oidc/README.md](../archbase-security-oidc/README.md) |
| Control Plane (token de sessão, licença) | [../archbase-controlplane/README.md](../archbase-controlplane/README.md) |

---

## Visão geral

- **Login por e-mail e senha**, com resposta JWT (`access_token` + `refresh_token`) e o tenant em que
  a autenticação aconteceu.
- **Access e refresh persistidos** em banco: logout, troca de senha e revogação de sessão valem de
  imediato, inclusive para o refresh.
- **Sessão única ou múltiplas sessões**, por usuário.
- **MFA (TOTP)** opcional, por usuário, com códigos de recuperação.
- **Token de API** (UUID, gravado como hash) para integração serviço a serviço.
- **Reset de senha** por código enviado por e-mail — o envio é da aplicação (`ArchbaseEmailService`).
- **Ganchos de negócio**: `AuthenticationBusinessDelegate` e `AuthenticationResponseEnricher`.
- **Login social** só com um `ArchbaseSocialTokenValidator` registrado pela aplicação; o framework
  não traz nenhum.
- **Provedor externo** (IdP OIDC, Control Plane) pela SPI `ExternalBearerAuthenticator`, em módulos
  opt-in.
- **Cadeia de segurança** padrão (`DefaultArchbaseSecurityConfiguration`) substituível por uma classe
  da aplicação.

---

## Propriedades obrigatórias

```properties
# Chave HS256 do JWT: Base64 padrão, >= 32 bytes depois de decodificada. Sem valor padrão.
archbase.security.jwt.secret-key=${JWT_SECRET}
# Validade, em milissegundos. Sem valor padrão.
archbase.security.jwt.token-expiration=3600000
archbase.security.jwt.refresh-expiration=604800000

# Lidas pela configuração padrão (DefaultArchbaseSecurityConfiguration). Sem valor padrão:
# a aplicação não sobe sem elas, mesmo que a whitelist fique vazia.
archbase.security.whitelist=
archbase.security.cors.allowed-origins=https://app.exemplo.com
archbase.security.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
archbase.security.cors.allowed-headers=Authorization,Content-Type,X-TENANT-ID,X-COMPANY-ID
archbase.security.cors.allow-credentials=true
```

> ⚠ `archbase.security.jwt.secret` e `archbase.security.jwt.expiration` **não existem**. Versões
> anteriores da documentação as citavam; não têm efeito.

A whitelist da configuração padrão já inclui `/api/v1/auth/**`, `/api/v1/apiToken/activate` e
`/error`, além de Swagger, `/actuator/**`, arquivos estáticos e duas rotas legadas — cada grupo
controlado por `archbase.security.public-paths.*`. O valor de `archbase.security.whitelist` é
**somado** a isso. Na subida sai uma linha de log com a superfície anônima completa.

---

## Login

### Endpoints

Todos em `/api/v1/auth`, anônimos (dentro de `/api/v1/auth/**`).

| Método e rota | Corpo | Observação |
|---|---|---|
| `POST /authenticate` | `email`, `password`, `tenantId` (opcional) | o login básico |
| `POST /login` | `email`, `password`, `context`, `contextData`, `tenantId` (opcional) | chama o delegate e os enrichers (ver [Ganchos](#ganchos-de-regra-de-negócio)) |
| `POST /login-flexible` | `identifier`, `password`, `context`, `tenantId` (opcional) | `identifier` precisa ser e-mail — telefone é recusado (`Login por telefone requer implementação específica`) |
| `POST /login-social` | `provider`, `token`, `context` | exige `ArchbaseSocialTokenValidator`; sem ele responde `501 SOCIAL_LOGIN_NOT_CONFIGURED` |
| `POST /mfa/verify` | `challengeToken`, `code` | segundo passo do MFA |
| `POST /refresh-token` | `token` | renovação (ver [Refresh](#refresh)) |
| `POST /register` | `name`, `email`, `password`, `avatar`, `additionalData` | auto-cadastro anônimo; desligue com `archbase.security.public-paths.registration=false` |
| `GET /tenants?email=` | — | descoberta de tenants (ver [Tenant no login](#tenant-no-login)) |
| `GET /contexts`, `GET /contexts/{context}/validate` | — | contextos do delegate |
| `POST /sendResetPasswordEmail/{email}`, `POST /resetPassword` | — | reset de senha (ver [Senha](#senha)) |

`contextData` existe no corpo de `/login`, mas nenhum código do framework o lê.

### O que acontece no `POST /authenticate`

1. **Contagem de tentativas** por e-mail (`archbase.security.rate-limit.*`). Bloqueado → `429` com
   `Retry-After`.
2. **Tenant**: o do corpo; sem ele, o da linha do usuário com aquele e-mail (ver
   [Tenant no login](#tenant-no-login)).
3. **Senha** conferida pelo `AuthenticationManager` (`DaoAuthenticationProvider` + BCrypt). Conta
   desativada, bloqueada ou com credencial expirada é recusada aqui.
4. **Senha local desligada** para a conta (SPI `ArchbaseLocalCredentialPolicy`, usada quando a
   credencial é de um IdP externo) → mesma resposta de senha errada.
5. **MFA** habilitado para o usuário → devolve `mfa_required: true` e um `challenge_token`, sem
   tokens de sessão. O cliente completa em `POST /api/v1/auth/mfa/verify`.
6. **Sessão** (ver [Sessão única ou múltiplas](#sessão-única-ou-múltiplas-sessões)): reaproveita o
   access token ainda válido ou revoga os anteriores; emite um refresh novo.
7. **Resposta**.

### Resposta

```json
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 1767225600000,
  "id_token": "6f1c...",
  "token_type": "BEARER",
  "user": { "...": "..." },
  "tenant": { "tenantId": "a9f8...", "nome": null, "descricao": null }
}
```

- `expires_in` é o **instante** de expiração do access token, em epoch milissegundos — não uma
  duração.
- `id_token` é o id da linha do access token em banco, **não** um ID token OIDC.
- `user` sai sem o hash da senha.
- No desafio de MFA vêm só `mfa_required` e `challenge_token`.

### Erros

| Situação | Resposta |
|---|---|
| e-mail inexistente ou senha errada | `401`, corpo `Login ou senha inválido` — mesmo corpo e mesmo tempo nos dois casos |
| credencial expirada (troca obrigatória ou prazo vencido) | `403` com `error: CREDENTIALS_EXPIRED` e `requirePasswordChange: true` |
| excesso de tentativas | `429` com `Retry-After` e `error: TOO_MANY_ATTEMPTS` |

---

## Sessões: access token e refresh token

Os dois são JWT HS256 assinados com `archbase.security.jwt.secret-key` e **os dois são persistidos**
em `AccessTokenEntity`, distinguidos pela coluna de uso (`TokenUse.ACCESS` / `TokenUse.REFRESH`) e
pelo claim `token_use` (`access`, `refresh`, `mfa_challenge`). O JWT também carrega o claim
`tenantId`, que é a fonte de verdade do tenant da requisição.

### Uso nas requisições

```http
GET /api/v1/qualquer-coisa
Authorization: Bearer <access_token>
```

O `ArchbaseJwtAuthenticationFilter` aceita **só access token**: refresh e desafio de MFA são
recusados como credencial. Um access token vale se a assinatura e a validade conferem **e** a linha
em banco existe e não está expirada nem revogada.

- `X-TENANT-ID` divergente do tenant do token não troca de tenant: com JWT próprio ou token de API a credencial
  é procurada no tenant do cabeçalho, não é achada e a requisição segue **não autenticada (401)**; com token de IdP
  externo, **403 `TENANT_MISMATCH`**.
- Token com assinatura válida mas revogado (logout, outro login de sessão única) → `401` com corpo
  `{"status":401,"error":"SESSAO_ENCERRADA",...}`. Sem token válido, `401`; autenticado sem
  permissão, `403`.
- `?token=` na URL também é aceito enquanto `archbase.security.jwt.accept-token-query-param=true`
  (padrão). Token em URL vai para log de acesso e `Referer`; desligue quando puder.
- Tokens emitidos antes do claim `token_use` são aceitos enquanto
  `archbase.security.jwt.strict-token-use=false` (padrão).

### Refresh

```http
POST /api/v1/auth/refresh-token
Content-Type: application/json

{ "token": "<refresh_token>" }
```

O campo do corpo é **`token`**. O refresh precisa ter `token_use=refresh`, assinatura válida e uma
linha viva em banco — refresh revogado continua com assinatura boa, e é a linha que o derruba. O
estado da conta é conferido de novo: desativada ou bloqueada → `403 ACCOUNT_DISABLED`; credencial
expirada → `403 CREDENTIALS_EXPIRED`. Cada renovação **rotaciona**: o refresh apresentado deixa de
valer e um par novo é emitido.

### Sessão única ou múltiplas sessões

A escolha é **por usuário**, na coluna `BO_PERMITE_MULTIPLICOS_LOGINS` (`allowMultipleLogins`) da
tabela `SEGURANCA`. Não existe propriedade global — `archbase.security.allow-multiple-logins` não
existe.

| Evento | `allowMultipleLogins` diferente de `S` (sessão única) | `allowMultipleLogins = 'S'` |
|---|---|---|
| login com access token ainda válido | revoga tudo do usuário e emite par novo | reaproveita o access e emite um refresh novo, sem tocar nas outras sessões |
| login sem access token válido | revoga tudo e emite par novo | revoga tudo e emite par novo |
| refresh | revoga tudo e emite par novo | revoga só o refresh apresentado |
| verificação de MFA, login social | revoga tudo | revoga tudo |
| logout | revoga **todos** os tokens do usuário | revoga **todos** os tokens do usuário |
| reset de senha por código | revoga tudo | revoga tudo |
| troca autenticada de senha | só com `archbase.security.password-change.revoke-sessions=true` | idem |

---

## Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <access_token>
```

Registrado pela própria cadeia de segurança (`LogoutFilter` do Spring + `ArchbaseLogoutService`) e
responde `200`. Revoga **todos** os tokens do dono do token apresentado — access e refresh, de todas
as sessões dele —, não só o access atual.

```properties
archbase.security.logout.enabled=true            # false se a aplicação tem o próprio logout
archbase.security.logout.url=/api/v1/auth/logout
```

O `LogoutFilter` intercepta a URL antes do `DispatcherServlet`: um controller da aplicação mapeado
no mesmo caminho deixa de ser chamado. Mude a URL ou desligue.

---

## MFA (TOTP)

Opcional e por usuário (`BO_MFA_HABILITADO`). Sem MFA habilitado, o login não muda.

**Configuração pelo próprio usuário** — `MfaController`, em `/api/v1/mfa`, exige autenticação:

| Rota | O que faz |
|---|---|
| `GET /api/v1/mfa/status` | `{"enabled": true/false}` |
| `POST /api/v1/mfa/setup` | gera o segredo e a URI do QR Code; o MFA ainda não está ativo |
| `POST /api/v1/mfa/enable` | corpo `{"code": "123456"}`: confirma o primeiro código, ativa e devolve 10 códigos de recuperação |
| `POST /api/v1/mfa/disable` | desativa e apaga segredo e códigos |

**Login em duas etapas**: o `/authenticate` (ou `/login`) devolve `mfa_required` e
`challenge_token` (validade de 5 minutos); o cliente envia
`POST /api/v1/auth/mfa/verify` com `{"challengeToken": "...", "code": "..."}`. O código pode ser o
TOTP ou um código de recuperação, que é consumido no uso. As tentativas são contadas
(`archbase.security.rate-limit.*`).

```properties
archbase.security.mfa.issuer=Minha Aplicação   # nome exibido no app autenticador (padrão: Archbase)
archbase.security.crypto.key=${CRYPTO_KEY}    # chave AES do segredo TOTP em repouso
```

O segredo TOTP é gravado cifrado (AES-GCM, `ArchbaseCryptoService`); os códigos de recuperação,
como hash BCrypt. `archbase.security.crypto.key` não tem valor padrão útil: forneça por variável de
ambiente ou secret manager.

O login social (`/login-social`) também passa pelo MFA: usuário com MFA habilitado recebe o mesmo
desafio do login por senha.

---

## Token de API

Credencial de longa duração para integração. É um **UUID**, ligado a um usuário — a requisição
roda como esse usuário, com as permissões dele.

```http
Authorization: Bearer 3f2b8c1e-0d4a-4c55-9b7e-2a1f6c9d8e70
```

O filtro também aceita o UUID cru no cabeçalho (`Authorization: 3f2b8c1e-...`) e em `?token=`
(sujeito a `accept-token-query-param`). Não existe prefixo `ApiToken`.

| Rota | O que faz |
|---|---|
| `POST /api/v1/apiToken/create?email=&expirationDate=&name=&description=` | cria o token (inativo) e devolve o valor **uma única vez** |
| `GET /api/v1/apiToken/activate?token=&tenantId=` | ativa; anônima, faz parte da whitelist padrão |
| `POST /api/v1/apiToken/revoke?token=` | revoga |

- A criação tenta enviar o token por e-mail (`ArchbaseEmailService.sendActivationTokenApiEmail`);
  falha no envio não desfaz a criação.
- Por padrão só o hash SHA-256 é gravado (`TOKEN_HASH`); o valor em claro não é recuperável depois.
  `archbase.security.api-token.hash-enabled=false` volta a gravá-lo em claro.
- Vale se estiver ativado, não revogado e dentro de `DH_EXPIRACAO`. O tenant da linha do token é a
  fonte de verdade; com `X-TENANT-ID` divergente o token é procurado no tenant do cabeçalho, não é achado → `401`.
- Os endpoints de criação, revogação e listagem são administrativos (`@ArchbaseSecurityAdminEndpoint`):
  quem pode chamá-los depende de `archbase.security.admin-endpoints.policy`.

---

## Senha

### Reset por código (esqueci a senha)

```http
POST /api/v1/auth/sendResetPasswordEmail/usuario@exemplo.com
```

```http
POST /api/v1/auth/resetPassword
Content-Type: application/json

{ "email": "usuario@exemplo.com", "passwordResetToken": "48213907", "newPassword": "..." }
```

- O código tem **8 dígitos** e vale **1 hora** — fixo no código, sem propriedade.
- Pedir um código novo revoga os anteriores; o código é de uso único.
- O e-mail é enviado pela implementação de `ArchbaseEmailService` **da aplicação**. O bean padrão
  lança `UnsupportedOperationException`: sem implementação, o pedido falha. Configuração de SMTP
  (`spring.mail.*`) e remetente são da implementação da aplicação — `archbase.email.from` não
  existe.
- Usuário com `BO_PERMITE_ALTERAR_SENHA = 'N'` não recebe código.
- Com `archbase.security.prevent-user-enumeration=true`, o pedido de código responde igual para
  e-mail cadastrado, não cadastrado, sem permissão de troca ou com falha de envio. No
  `/resetPassword`, e-mail inexistente responde como código errado (`Token de redefinição de senha
  inválido.`) e a tentativa é contada.
- Tentativas de código erradas são contadas (`archbase.security.rate-limit.*`).
- Reset bem-sucedido aplica a política de força, zera a troca obrigatória, reinicia a contagem de
  validade e **revoga todas as sessões** do usuário.

### Troca autenticada

```http
PATCH /api/v1/user
Authorization: Bearer <access_token>

{ "currentPassword": "...", "newPassword": "...", "confirmationPassword": "..." }
```

Exige a senha atual. Não desloga, salvo com `archbase.security.password-change.revoke-sessions=true`.
Senha atual errada, confirmação diferente, senha nova fora da política ou conta com senha de outro provedor
respondem **400** com a mensagem (`ArchbasePasswordChangeRejectedException`); até a 3.5.0 respondiam 500.
Não existe `/api/v1/auth/changePassword`.

### Força da senha

Inteiramente desligada por padrão. Vale para reset e troca autenticada.

```properties
archbase.security.password.min-length=0
archbase.security.password.require-digit=false
archbase.security.password.require-uppercase=false
archbase.security.password.require-lowercase=false
archbase.security.password.require-special=false
archbase.security.password.block-common=false
```

`archbase.security.password.policy.*` não existe.

### Expiração e troca obrigatória

O Spring Security consulta `UserDetails#isCredentialsNonExpired()` antes de autenticar. A regra,
nesta ordem:

| Condição | Resultado |
|----------|-----------|
| `BO_ALTERAR_SENHA_PROXIMO_LOGIN = 'S'` (`changePasswordOnNextLogin`) | credenciais **expiradas** — força a troca |
| `BO_SENHA_NUNCA_EXPIRA = 'S'` (`passwordNeverExpires`) ou nulo | credenciais válidas |
| `BO_SENHA_NUNCA_EXPIRA = 'N'` e `archbase.security.password.expiration-days = 0` (padrão) | credenciais válidas (política desligada) |
| `BO_SENHA_NUNCA_EXPIRA = 'N'` e senha mais antiga que o prazo | credenciais **expiradas** |

A idade da senha é medida a partir de `DT_ULTIMA_TROCA_SENHA`, atualizada pelo reset por código e
pela troca autenticada. **Sem essa data, vale a data de criação do registro (`DH_CRIACAO`)** — ligar
a política numa base antiga expira de uma vez quem tem `BO_SENHA_NUNCA_EXPIRA = 'N'` e foi criado há
mais tempo que o prazo. Só não expira por tempo quem não tem nenhuma das duas datas.

Credencial expirada responde `403 CREDENTIALS_EXPIRED` no login e no refresh.

> **Mudança de comportamento (3.0.x → 3.1).** Até a 3.0.x, `isCredentialsNonExpired()` devolvia
> `passwordNeverExpires` diretamente: todo usuário com `BO_SENHA_NUNCA_EXPIRA = 'N'` ficava com
> "credenciais expiradas agora" e não autenticava nem logo depois de um reset. Para forçar a troca,
> use `BO_ALTERAR_SENHA_PROXIMO_LOGIN = 'S'`; para expiração periódica,
> `archbase.security.password.expiration-days`.

---

## Tenant no login

### O cliente não precisa saber o tenant antes de logar

No modelo do Archbase, **um e-mail em N tenants são N linhas de usuário** — cada uma na tabela
`SEGURANCA` com o seu `TENANT_ID`, o seu perfil, os seus grupos e **a sua própria senha**. Não existe
entidade de vínculo "usuário ↔ tenants", e não existe tabela de tenant.

Isso criava um bootstrap circular no frontend: para logar era preciso mandar `X-TENANT-ID`, mas o
tenant só se descobre depois de saber quem é a pessoa. Na prática as aplicações resolviam embutindo o
tenant numa variável de build (`VITE_TENANT_ID`), o que fixa um tenant por build.

**Agora o servidor informa.** Toda resposta de login bem-sucedido traz o tenant em que a autenticação
aconteceu:

```http
POST /api/v1/auth/authenticate
{ "email": "usuario@exemplo.com", "password": "senha123" }

Response 200:
{
  "access_token": "...",
  "refresh_token": "...",
  "user": { ... },
  "tenant": {
    "tenantId": "a9f814d2-4dae-41f3-851b-8aa3d4706561",
    "nome": null,
    "descricao": null
  }
}
```

Vale para `/authenticate`, `/login`, `/login-flexible`, `/login-social` e `/refresh-token`. Não vem na
resposta de desafio de MFA, onde o login ainda não se completou. O valor é o mesmo do claim
`tenantId` do JWT, que continua sendo a fonte de verdade inforjável — o campo só o torna legível sem
decodificar o token.

O login já resolvia o tenant a partir do e-mail quando o pedido não trazia `tenantId`; o que faltava
era devolvê-lo. A resolução funciona assim:

| Pedido | Resultado |
|---|---|
| `tenantId` no corpo de `/authenticate`, `/login` ou `/login-flexible` | usado — tem precedência |
| sem `tenantId`, e-mail em **um** tenant | o tenant da linha do usuário |
| sem `tenantId`, e-mail em **mais de um** tenant | `400` com `{"error": "TENANT_SELECTION_REQUIRED", "message": "Selecione o tenant para efetuar login"}` (em `/authenticate`, `/login` e `/login-flexible`) |

> ⚠ **O cabeçalho `X-TENANT-ID` não entra nessa resolução.** A busca do tenant pelo e-mail é uma
> consulta nativa que ignora o tenant do contexto. Para e-mail em vários tenants, mande `tenantId`
> **no corpo**.

O login roda numa sessão do Hibernate aberta **depois** de o tenant resolvido estar no contexto. Até a 3.5.0 a
sessão abria antes — na entrada do método transacional, ou já no começo da requisição pelo open-in-view —, com o
tenant do cabeçalho ou o padrão: sem `X-TENANT-ID`, quem não era do tenant padrão recebia `Login ou senha inválido`,
inclusive com `tenantId` no corpo ou com o tenant descoberto pelo e-mail. Até a 3.5.0, `/login` e `/login-flexible`
também não aceitavam `tenantId`.

### Rótulo das organizações: quem tem o cadastro é a aplicação

`GET /api/v1/auth/tenants?email=` lista os tenants de um e-mail, para telas que precisam de seleção
antes do login. Ele devolve **apenas o `tenantId`**:

```json
[ { "tenantId": "a9f814d2-...", "nome": null, "descricao": null } ]
```

Antes, `nome` e `descricao` vinham das colunas `NOME`/`DESCRICAO` da linha de `SEGURANCA` — que, para
`TP_SEGURANCA='USUARIO'`, são **o nome e a descrição da pessoa**. O endpoint é anônimo: qualquer um
que soubesse um e-mail recebia de volta o nome do titular. E o seletor de tenant do cliente acabava
exibindo o nome do próprio usuário no lugar da empresa.

O framework não tem cadastro de organizações, então parou de inventar o rótulo. Quem tem esse
cadastro é a aplicação:

```java
@Bean
public ArchbaseTenantInfoResolver tenantInfoResolver(OrganizacaoRepository repository) {
    return tenantId -> repository.findById(tenantId)
            .map(org -> TenantLoginOption.builder()
                    .nome(org.getNomeFantasia())
                    .descricao(org.getRazaoSocial())
                    .build())
            .orElse(null);
}
```

Com o bean registrado, os rótulos passam a vir preenchidos **tanto na descoberta quanto na resposta
do login**. Sem ele, o cliente recebe o id e decide como exibi-lo. O `tenantId` sempre vem do banco,
mesmo que o resolver não o preencha; devolver `null` é válido e significa "não tenho rótulo para
este".

> **Contrato:** o resolver é consultado em fluxo anônimo (pré-login). Não deve devolver informação
> que exija autenticação. Exceção lançada por ele é registrada em log e não derruba o login — o id
> sozinho já é suficiente para o cliente funcionar.

### Contagem de tentativas na descoberta

`GET /api/v1/auth/tenants` é anônimo e responde sobre a existência de um e-mail, então tem contagem
própria, em **duas chaves**, e qualquer uma basta para recusar com `429` + `Retry-After`:

| chave | contém |
|---|---|
| origem (IP) | **enumeração** — varredura usa um e-mail diferente por palpite, o que daria orçamento novo a cada tentativa se a contagem fosse só por e-mail |
| e-mail | martelo sobre um alvo específico, vindo de várias origens |

O escopo é separado do `login`: abusar da descoberta **não** tranca o login legítimo de quem tem
aquele e-mail — seria negação de serviço contra a vítima. Usa as chaves
`archbase.security.rate-limit.*` já existentes.

### Enumeração por tempo no login

E-mail inexistente e senha errada devolvem o **mesmo** `401` com o mesmo corpo
(`Login ou senha inválido`) — e agora também custam o **mesmo tempo**.

O `DaoAuthenticationProvider` do Spring já se defende disso: quando o usuário não existe, ele confere
a senha apresentada contra um hash fictício e descarta o resultado, só para gastar o mesmo bcrypt.
Mas esse ramo só roda em `catch (UsernameNotFoundException)`. O bean padrão do Archbase resolvia o
usuário com `Optional.get()`, que lança `NoSuchElementException` — a mitigação nunca era alcançada, e
o e-mail inexistente respondia rápido demais.

> **Se a sua aplicação registra o próprio `UserDetailsService`**, o bean do framework
> (`@ConditionalOnMissingBean`) não entra, e essa proteção é sua responsabilidade: lance
> `UsernameNotFoundException` — não `NoSuchElementException`, não `null`, não uma exceção própria.


---

## Ganchos de regra de negócio

Dois pontos de extensão para a aplicação acrescentar lógica própria ao login e ao cadastro.

### `AuthenticationBusinessDelegate`

```java
public interface AuthenticationBusinessDelegate {

    // Depois do cadastro: cria a entidade de negócio e devolve o id dela
    String onUserRegistered(User user, Map<String, Object> registrationData);

    // Enriquece a resposta do login
    AuthenticationResponse enrichAuthenticationResponse(
        AuthenticationResponse baseResponse, String context, HttpServletRequest request);

    boolean supportsContext(String context);
    List<String> getSupportedContexts();

    default void preAuthenticate(String email, String context) { }
    default void postAuthenticate(User user, String context) { }
    default String onSocialLogin(String provider, Map<String, Object> providerData) { return null; }
    default boolean validatePasswordRecoveryCode(String email, String code) { return true; }
    default String getDefaultContext() { return "DEFAULT"; }
}
```

Sem implementação da aplicação, vale o `DefaultAuthenticationBusinessDelegate`, que não faz nada e
só conhece o contexto `DEFAULT`. Nomes como `STORE_APP`, `CUSTOMER_APP` ou `WEB_ADMIN` são exemplos:
**a aplicação define os seus** em `getSupportedContexts()`.

```java
@Component
@Primary
public class MinhaAppAuthenticationDelegate implements AuthenticationBusinessDelegate {

    @Override
    public String onUserRegistered(User user, Map<String, Object> registrationData) {
        UserApp userApp = UserApp.builder()
            .securityUser(user)
            .name((String) registrationData.get("name"))
            .build();
        return userAppService.save(userApp).getId();
    }

    @Override
    public AuthenticationResponse enrichAuthenticationResponse(
            AuthenticationResponse baseResponse, String context, HttpServletRequest request) {
        return switch (context) {
            case "STORE_APP" -> enrichStoreResponse(baseResponse);
            default -> baseResponse;
        };
    }

    @Override
    public boolean supportsContext(String context) {
        return getSupportedContexts().contains(context);
    }

    @Override
    public List<String> getSupportedContexts() {
        return List.of("STORE_APP", "WEB_ADMIN");
    }
}
```

`@Primary` evita depender da ordem do scan: o delegate padrão é um `@Component` com
`@ConditionalOnMissingBean`, condição que não é confiável fora de autoconfiguração.

### `AuthenticationResponseEnricher`

```java
public interface AuthenticationResponseEnricher {
    AuthenticationResponse enrich(AuthenticationResponse baseResponse, String context,
                                  HttpServletRequest request);
    default boolean supports(String context) { return true; }
    default int getOrder() { return 0; }   // menor roda primeiro
}
```

Pode haver vários. Exceção num enricher é registrada em log e não derruba o login.

### Quem chama o quê

Os ganchos **não** rodam igual em todos os endpoints de login:

| Endpoint | `preAuthenticate` | `postAuthenticate` | `enrichAuthenticationResponse` | enrichers |
|---|---|---|---|---|
| `/authenticate` | — | — | — | — |
| `/login` | se `context` veio | se `context` veio | se `context` veio | se `context` veio |
| `/login-flexible` | sim | sim | sim | — |
| `/login-social` | — | — | sim | — |

Em `/login` a ordem é: `preAuthenticate` → autenticação → (desafio de MFA volta aqui, sem ganchos)
→ `postAuthenticate` → `enrichAuthenticationResponse` → enrichers, por `getOrder()`. Falha no
`preAuthenticate` recusa o login (`Acesso negado para este contexto`); falha nos demais é só
registrada.

`/login-flexible` recusa com `400` o contexto que o delegate não suporta. Como no `/login`, o
desafio de MFA volta antes de `postAuthenticate` e do enriquecimento — sem usuário, não há o que
pós-processar. O mesmo vale para `/login-social`.

O cadastro (`/register`) chama `onUserRegistered` **uma vez**, com os campos do cadastro mais o
`additionalData` do corpo; em colisão de chave, vale o campo do cadastro. Exceção no gancho é
registrada em log e não desfaz o usuário criado — a implementação ainda deve ser idempotente.

O login social cadastra automaticamente o e-mail que ainda não existe (senha aleatória; nome e
descrição vêm do `name` do provedor ou, na falta dele, do e-mail; a foto só é usada se vier em bytes)
e então chama `onSocialLogin` com `newUser=true`. O padrão de `onSocialLogin` não faz nada.

> Antes da 3.5.0 estes três pontos eram diferentes: `onUserRegistered` rodava duas vezes com
> `additionalData`, o `/login-flexible` repassava o desafio de MFA aos ganchos, e o primeiro login
> social de qualquer e-mail novo falhava (descrição obrigatória vazia e `onSocialLogin` padrão
> lançando exceção).

---

## Provedor de identidade externo

Login por um IdP externo não substitui o `archbase-security`: o token de fora prova **quem** é a
pessoa; **o que** ela pode fazer continua decidido aqui. O ponto de entrada é a SPI
`ExternalBearerAuthenticator` (`br.com.archbase.security.auth.external`): o
`ArchbaseJwtAuthenticationFilter` entrega ao autenticador externo o bearer que ele reconhece, e o
resto do filtro não roda para aquela requisição. Sem nenhum autenticador registrado, nada muda.

Dois módulos opt-in implementam a SPI:

- **`archbase-security-oidc`** — access token de IdP OIDC (ArchGuard, Keycloak, Entra ID, Auth0,
  Okta), ao lado do login próprio. Ver [../archbase-security-oidc/README.md](../archbase-security-oidc/README.md).
- **`archbase-controlplane`** — token de sessão do Control Plane, licença e assentos. Ver
  [../archbase-controlplane/README.md](../archbase-controlplane/README.md).

A pesquisa em `docs/archive/keycloak/` é histórica; as decisões de lá não valem para a implementação.

---

## Customizar a configuração de segurança

### Quando a padrão é suficiente

A cadeia padrão é a `DefaultArchbaseSecurityConfiguration`. Antes de substituí-la, veja se o que
você precisa já é propriedade: endpoints públicos (`archbase.security.whitelist`,
`archbase.security.public-paths.*`), CORS (`archbase.security.cors.*`), logout
(`archbase.security.logout.*`). Substituir a cadeia faz você **perder todas essas propriedades**.

### Passo 1: crie a classe

Estenda `BaseArchbaseSecurityConfiguration` e implemente `CustomSecurityConfiguration` — uma
interface marcadora, sem métodos. A `DefaultArchbaseSecurityConfiguration` é
`@ConditionalOnMissingBean(CustomSecurityConfiguration.class)`: com a sua classe registrada, ela sai.

```java
package seu.pacote.config;

import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import br.com.archbase.security.config.BaseArchbaseSecurityConfiguration;
import br.com.archbase.security.config.CustomAccessDeniedHandler;
import br.com.archbase.security.config.CustomSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.util.List;

@Configuration
@EnableWebSecurity
public class MinhaConfiguracaoSeguranca extends BaseArchbaseSecurityConfiguration
                                        implements CustomSecurityConfiguration {

    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter;

    public MinhaConfiguracaoSeguranca(CustomAccessDeniedHandler accessDeniedHandler,
                                      ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter) {
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // métodos do Passo 2
}
```

### Passo 2: implemente os métodos abstratos

```java
@Override
protected CustomAccessDeniedHandler getAccessDeniedHandler() {
    return accessDeniedHandler;
}

@Override
protected ArchbaseJwtAuthenticationFilter getJwtAuthenticationFilter() {
    return jwtAuthenticationFilter;
}

@Override
protected List<String> getAllowedOrigins() {
    return List.of("https://app.seudominio.com", "http://localhost:3000");
}

@Override
protected List<String> getAllowedMethods() {
    return List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
}

@Override
protected List<String> getAllowedHeaders() {
    return List.of("Authorization", "Content-Type", "X-TENANT-ID", "X-COMPANY-ID");
}

@Override
protected boolean getAllowCredentials() {
    return true;
}

/**
 * A lista INTEIRA do que fica anônimo. Nada da whitelist padrão é herdado: sem
 * /api/v1/auth/** ninguém faz login, e sem /error o status de erro vira 403.
 */
@Override
protected List<String> getWhiteListUrls() {
    return List.of(
        "/api/v1/auth/**",
        "/api/v1/apiToken/activate",
        "/error",
        "/actuator/health",
        "/v3/api-docs/**",
        "/swagger-ui/**"
    );
}

/** Exceções que voltam a exigir autenticação dentro de um padrão liberado. */
@Override
protected List<String> getProtectedPathsWithinWhitelist() {
    return List.of("/api/v1/auth/register");   // sem auto-cadastro anônimo
}

/** Regras de URL. Roda DEPOIS da whitelist: no Spring Security vence a primeira regra que casa. */
@Override
protected void configureAuthorizationRules(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/interno/**").denyAll()
        .anyRequest().authenticated()
    );
}
```

> ⚠ **Não use `hasRole(...)`, `hasAnyRole(...)` nem `hasAuthority(...)`.** O `UserEntity` devolve
> uma lista **vazia** em `getAuthorities()` — a autorização do Archbase não passa por authorities do
> Spring. Uma regra `.hasRole("ADMIN")` **nega sempre**, inclusive para o administrador. Nas regras de
> URL, fique em `permitAll()`, `authenticated()` e `denyAll()`; a decisão fina vai nos métodos, com
> [`@HasPermission`](#1-haspermission--a-que-concede) e as trancas `@RequireProfile` / `@RequireRole`.

> ⚠ **`@PermitAll`, `@RolesAllowed` e `@Secured` são ignorados.** O `MethodSecurityConfig` liga só
> `prePostEnabled`. Endpoint público vai na whitelist.

### Passo 3: o `configure` — use os ganchos, não sobrescreva

`BaseArchbaseSecurityConfiguration.configure(HttpSecurity)` monta a cadeia inteira:

1. CSRF desligado; CORS a partir de `getAllowed*()`;
2. `getProtectedPathsWithinWhitelist()` → `authenticated()`, **antes** da whitelist;
3. `getWhiteListUrls()` → `permitAll()`; `OPTIONS /**` → `permitAll()`;
4. `configureAuthorizationRules(http)`;
5. sessão `STATELESS`;
6. logout em `getLogoutUrl()` com o `ArchbaseLogoutService`, se `archbase.security.logout.enabled`;
7. `ArchbaseJwtAuthenticationFilter` antes do `UsernamePasswordAuthenticationFilter`;
8. `getAuthenticationEntryPoint()` (401, com `SESSAO_ENCERRADA`) e `getAccessDeniedHandler()` (403);
9. uma linha de log com a superfície anônima e um WARN se `/actuator` estiver liberado.

Os padrões são `String` passados a `requestMatchers(...)`. O Spring Security 7 não tem o
`AntPathRequestMatcher` que exemplos antigos desta documentação usavam.

**Sobrescrever `configure` sem chamar `super` perde tudo o que você não reescrever** — o mais fácil
de esquecer são o logout (o cliente recebe sucesso e o token continua valendo), o entry point (token
expirado vira `403` e o cliente nunca renova) e as exceções da whitelist. Para acrescentar algo,
chame `super` primeiro:

```java
@Override
public void configure(HttpSecurity http) throws Exception {
    super.configure(http);
    http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
}
```

Os outros ganchos sobrescrevíveis:

| Método | Padrão |
|---|---|
| `getProtectedPathsWithinWhitelist()` | lista vazia |
| `getLogoutUrl()` | `archbase.security.logout.url` |
| `getAuthenticationEntryPoint()` | `new CustomAuthenticationEntryPoint()`; o tipo de retorno exige uma subclasse dela |

### O que continua valendo com a configuração própria

As anotações (`@HasPermission`, `@RequireProfile`, `@RequireRole`, `@RequirePersona`) continuam
funcionando: o `MethodSecurityConfig` é importado pela autoconfiguração independentemente da cadeia
de filtros. O que **não** vale mais: `archbase.security.whitelist`, `archbase.security.cors.*` e
`archbase.security.public-paths.*`, que só a `DefaultArchbaseSecurityConfiguration` lê.

---

# Anotações de Segurança do Archbase

> **Referência completa:** [ARQUITETURA.md](ARQUITETURA.md). Esta seção é o resumo prático.

As anotações **não decidem** — elas declaram o que o endpoint exige. Quem decide é um avaliador
único, o `ArchbaseAccessEvaluator`, que roda cinco portões na mesma ordem, sempre:

```
1 IDENTITY   → 2 SCOPE → 3 RESTRICTION → 4 LEVEL → 5 GRANT
  nega          nega       nega            nega      CONCEDE
```

**A regra que organiza tudo: os portões 1 a 4 só sabem negar. Só o portão 5 concede.**

| Anotação | Declara | Pode conceder? | Precisa de |
|---|---|---|---|
| `@HasPermission` | uma capacidade (recurso + ação) | **sim** — é a única | catálogo alimentado |
| `@RequireProfile` | tranca por perfil | não, só nega | o usuário ter perfil |
| `@RequireRole` | tranca por papel do domínio | não, só nega | um `ArchbaseRoleResolver` |
| `@RequirePersona` | tranca por persona | não, só nega | mapeamento perfil → persona |
| `@ArchbaseResource` | o recurso da classe | — | vai na **classe** |

---

## 1. `@HasPermission` — a que concede

```java
@RestController
@RequestMapping("/api/v1/ordens-servico")
@ArchbaseResource(value = "tms.ordemservico", description = "Ordem de serviço")
public class OrdemServicoController {

    @GetMapping
    @HasPermission(action = "view", description = "Listar ordens de serviço")
    public ResponseEntity<Page<OrdemServicoDto>> listar() { ... }

    @PostMapping("/{id}/aprovar-custo")
    @HasPermission(action = "aprovar_custo", description = "Aprovar o custo da OS",
                   minimumLevel = AccessLevel.SUPERVISOR)
    public ResponseEntity<Void> aprovarCusto(@PathVariable String id) { ... }
}
```

**Parâmetros**

| Parâmetro | Obrigatório | O que faz |
|---|---|---|
| `action` | sim | nome da ação no catálogo |
| `description` | **sim** | vira a descrição da Action; é o que o admin lê ao conceder |
| `resource` | não | vazio herda de `@ArchbaseResource` na classe |
| `minimumLevel` | não | piso da capacidade; semeia `MINIMUM_LEVEL` no primeiro registro |
| `tenantId`, `companyId`, `projectId` | não | estreitamento de escopo |

> ⚠ **`description` não tem valor padrão.** Exemplos que a omitem — inclusive versões antigas desta
> documentação — **não compilam**.

> ⚠ **`@HasPermission` é `@Target(METHOD)`**, de propósito. Uma capacidade é *recurso + ação*, e a
> ação é sempre por método; na classe, todo método herdaria a mesma ação e um `DELETE` passaria a
> exigir apenas `view`. Para não repetir o recurso, use `@ArchbaseResource`.

### `minimumLevel` é semente, não lei

O valor é gravado em `SEGURANCA_ACAO.MINIMUM_LEVEL` no **primeiro registro** da ação e nunca
sobrescrito depois. A partir daí quem manda é o admin — igual já acontece com a descrição. O
desenvolvedor declara o piso que conhece; a operação ajusta o que conhece melhor, sem deploy.

A escala tem quatro degraus: `READER < OPERATOR < SUPERVISOR < TENANT_ADMIN`. O portão só é avaliado
com `archbase.security.access-level.enabled=true`.

**Piso, não substituto:** alcançar o nível não concede nada. O acesso continua dependendo de
permissão no catálogo; o nível apenas impede que uma concessão indevida valha.

---

## 2. `@RequireProfile`

```java
@RequireProfile("SUPERVISOR")
public void fecharCompetencia() { ... }

// Administrador NÃO é isento: a tranca vale para todos.
@RequireProfile(value = "AUDITORIA", allowSystemAdmin = false)
public void exportarTrilha() { ... }
```

| Parâmetro | Padrão | O que faz |
|---|---|---|
| `value` | — | perfis aceitos |
| `requireAll` | `false` | exige todos em vez de um |
| `allowSystemAdmin` | `true` | isenta o administrador **desta** tranca |
| `requireActiveUser` | `true` | exige conta ativa |
| `resource`, `action` | vazio | soma uma capacidade ao requisito |
| `message` | — | texto do 403 |

> ⚠ **O usuário tem um perfil só** (`UserEntity.profile` é `@ManyToOne`). `requireAll = true` com
> dois perfis é insatisfazível por construção.

---

## 3. `@RequireRole`

```java
@RequireRole("GESTOR_FROTA")
public void reatribuirVeiculo() { ... }
```

| Parâmetro | Padrão | O que faz |
|---|---|---|
| `value` | — | papéis aceitos |
| `requireAll` | `false` | exige todos em vez de um |
| `requirePlatformAdmin` | `false` | exige `isAdministrator` |
| `ownerOnly` | `false` | exige propriedade, respondida pelo SPI |
| `allowSystemAdmin` | `true` | isenta o administrador desta tranca |

**As roles são do domínio da aplicação, não do Archbase.** Quem as conhece é o
`ArchbaseRoleResolver` que o projeto registra:

```java
@Component
public class MinhasRoles implements ArchbaseRoleResolver {

    @Override
    public Set<String> resolveRoles(UserEntity user) {
        return colaboradorRepository.rolesDe(user.getId());
    }

    @Override
    public boolean isOwner(UserEntity user) {
        return colaboradorRepository.ehProprietario(user.getId());
    }
}
```

> ⚠ **Sem esse bean, `@RequireRole` não valida nada.** O comportamento é decidido por
> `archbase.security.require-role.no-resolver-policy`, cujo padrão é `permit` — ou seja, a anotação
> **libera qualquer usuário ativo, ignorando os próprios valores**. Registre o resolver e mude para
> `deny`.

> ⚠ `ownerOnly` com **mais de um** resolver registrado sempre nega: `isOwner(user)` não identifica o
> domínio, então nada liga a propriedade que um resolver afirma à role que outro forneceu.
> Consolide em um resolver que conheça os dois lados.

> ⚠ `allowSystemAdmin` (padrão `true`) libera o administrador **antes** de `requirePlatformAdmin`.
> A combinação não significa "admin E role".

---

## 4. `@RequirePersona`

**Evite em código novo.** O mapeamento de perfil para persona é uma tabela fixa embutida no
framework, com vocabulário de outro domínio:

| Persona | Casa com o perfil |
|---|---|
| `PLATFORM_ADMIN` | `ADMIN`, `PLATFORM_ADMIN` |
| `STORE_ADMIN` | `STORE_MANAGER`, `STORE_ADMIN` |
| `CUSTOMER` | `CUSTOMER`, `USER` |
| `DRIVER` | `DRIVER` |
| qualquer outra | perfil de mesmo nome |

> ⚠ **`context` e `contextData` não são lidos na decisão.** Estão na assinatura, não no
> comportamento.

> ⚠ **Não há ligação entre enrichers e estas anotações.** Versões anteriores desta documentação
> afirmavam que `@RequireRole` e `@RequirePersona` eram extensíveis via enrichers — não são. A
> extensão de `@RequireRole` é o `ArchbaseRoleResolver`.

---

## Combinando anotações

Somam — todas precisam permitir. **Não existe OR entre elas.**

```java
@PostMapping("/{id}/cancelar")
@RequireProfile(value = "SUPERVISOR", allowSystemAdmin = false)
@HasPermission(action = "cancelar", description = "Cancelar a OS",
               minimumLevel = AccessLevel.SUPERVISOR)
public ResponseEntity<Void> cancelar(@PathVariable String id) { ... }
```

Lê-se: *precisa ser SUPERVISOR (mesmo sendo admin), precisa ter a capacidade concedida, e precisa
alcançar o nível.*

### Uma tranca sozinha concede

```java
@RequireProfile("SUPERVISOR")   // sem @HasPermission
public void fecharCompetencia() { ... }
```

Sem capacidade declarada não há catálogo a consultar, então passar na tranca é a decisão inteira.
Qualquer pessoa com o perfil executa, sem que ninguém tenha concedido nada.

É legítimo quando a regra é mesmo "só o perfil X" — mas **não substitui** `@HasPermission`: não
aparece no catálogo, não pode ser concedido nem revogado pelo admin, e não muda sem deploy.

---

## Nível de classe

`@RequireProfile`, `@RequireRole` e `@RequirePersona` aceitam a classe; a anotação no método vence a
da classe. `@HasPermission` não — use `@ArchbaseResource` para o recurso compartilhado.

---

## Por que negou

A decisão carrega o motivo. Os códigos mais comuns:

| Código | O que significa |
|---|---|
| `NO_GRANT` | ninguém concedeu — nem direto, nem grupo, nem perfil |
| `LEVEL_TOO_LOW` | concessão existe, o nível não alcança |
| `EXPLICIT_DENY` | há uma permissão `DENY` alcançando o escopo |
| `OUT_OF_SCOPE` | a permissão existe, mas para outro tenant/empresa/projeto |
| `PROFILE_NOT_MATCHED` | perfil diferente do exigido |
| `ROLE_RESOLVER_MISSING` | `no-resolver-policy=deny` sem resolver registrado |
| `PRINCIPAL_NOT_SUPPORTED` | o principal não é `UserEntity` |

> **`NO_GRANT` num sistema recém-anotado** quase sempre significa **catálogo vazio**, não permissão
> faltando. Confira `archbase.security.scan-packages`.

A tabela completa e os endpoints de diagnóstico — incluindo **simulação de acesso de outra pessoa** —
estão em [ARQUITETURA.md](ARQUITETURA.md).

---

## Configuração

As anotações são configuradas por `MethodSecurityConfig` e interceptadas por um
`AuthorizationManager` cada, que monta o requisito e delega ao avaliador:

| Interceptador | Anotação |
|---|---|
| `CustomAuthorizationManager` | `@HasPermission` |
| `ProfileAuthorizationManager` | `@RequireProfile` |
| `RoleAuthorizationManager` | `@RequireRole` |
| `PersonaAuthorizationManager` | `@RequirePersona` |

A regra de cada tranca vive num `RestrictionEvaluator`; a composição e a decisão, no
`ArchbaseAccessEvaluator`.


---

## Endurecimento (hardening)

Várias proteções existem mas vêm **inertes por padrão**, para não quebrar quem atualiza: política dos
endpoints administrativos, `@RequireRole` sem resolver, `strict-token-use`, token em query string,
enumeração no reset, auto-cadastro anônimo, `/actuator/**` público, conferência do `X-COMPANY-ID`
(`archbase.security.company.validation=off|warn|enforce`, com a SPI `ArchbaseCompanyAccessResolver`),
entre outras. O que cada chave faz, em que ordem ligar e o que cada uma quebra está em
[../deployment/security-hardening.md](../deployment/security-hardening.md).

---

## Resolução de problemas

### A configuração personalizada não é aplicada

1. A classe precisa ser um bean (`@Configuration`) **e** implementar `CustomSecurityConfiguration` —
   é essa interface que tira a `DefaultArchbaseSecurityConfiguration` do contexto.
2. Ela precisa estar num pacote varrido pela aplicação, ou ser importada com `@Import`.
3. Confira a linha `Superfície anônima da API (...)` no log de subida: ela mostra a whitelist que a
   cadeia ativa realmente usa.

### Toda requisição autenticada responde 403 numa regra de URL

Quase sempre é `hasRole(...)` ou `hasAuthority(...)` em `configureAuthorizationRules`. Ver o aviso
no [Passo 2](#passo-2-implemente-os-métodos-abstratos).

### O cliente fica em laço de 401 / `SESSAO_ENCERRADA`

O usuário tem sessão única (`BO_PERMITE_MULTIPLICOS_LOGINS` diferente de `S`) e entrou em outro
lugar, ou alguém fez logout com o mesmo usuário. Ver
[Sessão única ou múltiplas](#sessão-única-ou-múltiplas-sessões).

### `NO_GRANT` em tudo depois de anotar os endpoints

Catálogo vazio, não permissão faltando: confira `archbase.security.scan-packages`. Ver
[Por que negou](#por-que-negou).
