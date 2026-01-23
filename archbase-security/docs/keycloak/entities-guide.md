# Guia Completo: Entidades do Keycloak

## Índice
1. [Introdução](#introdução)
2. [Hierarquia de Entidades](#hierarquia-de-entidades)
3. [Realms](#realms)
4. [Clients](#clients)
5. [Users](#users)
6. [Roles](#roles)
7. [Groups](#groups)
8. [Client Scopes](#client-scopes)
9. [Identity Providers](#identity-providers)
10. [User Federation](#user-federation)
11. [Authentication Flows](#authentication-flows)
12. [Tokens e Sessões](#tokens-e-sessões)
13. [Fluxo Completo de Autenticação](#fluxo-completo-de-autenticação)

---

## Introdução

O Keycloak é uma solução open-source de gerenciamento de identidade e acesso (IAM) que fornece autenticação, autorização e SSO (Single Sign-On). Este documento explica as principais entidades e como elas se relacionam.

---

## Hierarquia de Entidades

```
Keycloak Server
└── Realms (Domínios)
    ├── Clients (Aplicações)
    │   ├── Client Roles
    │   ├── Client Scopes
    │   └── Mappers
    ├── Users (Usuários)
    │   ├── Attributes
    │   ├── Credentials
    │   ├── Role Mappings
    │   └── Group Memberships
    ├── Roles
    │   ├── Realm Roles
    │   └── Client Roles
    ├── Groups (Grupos)
    │   ├── Attributes
    │   ├── Role Mappings
    │   └── Subgroups
    ├── Identity Providers (Provedores de Identidade)
    ├── User Federation (Federação de Usuários)
    └── Authentication (Configurações de Autenticação)
```

---

## Realms

### O que é?
Um **Realm** é um domínio isolado que gerencia um conjunto de usuários, aplicações, roles e groups. É a unidade de isolamento mais alta no Keycloak.

### Características
- Cada realm tem sua própria configuração
- Usuários e aplicações de um realm não podem acessar outro realm
- Cada realm tem seus próprios endpoints de autenticação
- Geralmente, uma organização usa um realm

### Exemplo de Estrutura
```
Keycloak
├── master (realm administrativo do Keycloak)
├── minha-aplicacao (realm da aplicação)
├── empresa-portal (outro realm)
└── desenvolvimento (realm de testes)
```

### URL Padrão
```
https://keycloak.exemplo.com.br/realms/{realm-name}
```

### Configurações Importantes
- **Login Settings**: customização da tela de login
- **Tokens**: tempo de expiração dos tokens
- **Sessions**: configurações de sessão
- **Security Defenses**: proteção contra brute force, etc.

---

## Clients

### O que é?
Um **Client** representa uma aplicação ou serviço que usa o Keycloak para autenticação.

### Tipos de Clients

#### 1. **Public Client**
- Aplicações frontend (SPA, Mobile Apps)
- Não podem manter segredos
- Exemplo: aplicação React, Angular, Vue

```yaml
Client ID: minha-aplicacao-frontend
Client Protocol: openid-connect
Access Type: public
Valid Redirect URIs: https://bi.exemplo.com.br/*
Web Origins: https://bi.exemplo.com.br
```

#### 2. **Confidential Client**
- Aplicações backend (APIs, serviços)
- Podem armazenar client secret com segurança
- Exemplo: API Java, Node.js backend

```yaml
Client ID: minha-aplicacao-api
Client Protocol: openid-connect
Access Type: confidential
Client Secret: a8f3d9e2-4c7b-11ed-bdc3-0242ac120002
Valid Redirect URIs: https://api.exemplo.com.br/callback
```

#### 3. **Bearer-only Client**
- Apenas validam tokens
- Não fazem login
- Exemplo: APIs REST puras

```yaml
Client ID: minha-aplicacao-resource-server
Access Type: bearer-only
```

### Configurações Importantes

#### Valid Redirect URIs
URLs permitidas para redirecionamento após login:
```
https://bi.exemplo.com.br/callback
https://bi.exemplo.com.br/*
http://localhost:3000/* (desenvolvimento)
```

#### Web Origins
URLs permitidas para CORS:
```
https://bi.exemplo.com.br
http://localhost:3000
```

#### Service Accounts
- Permite autenticação machine-to-machine
- Client pode obter token sem usuário

---

## Users

### O que é?
**Users** são as contas de usuários que podem se autenticar no sistema.

### Estrutura de um User

```json
{
  "id": "a063d40c-df1e-4c99-b291-137f1ca04a21",
  "username": "joao.silva",
  "email": "joao.silva@exemplo.com.br",
  "firstName": "João",
  "lastName": "Silva",
  "enabled": true,
  "emailVerified": false,
  "attributes": {
    "departamento": ["Financeiro"],
    "matricula": ["12345"],
    "telefone": ["44-99999-9999"]
  },
  "requiredActions": [],
  "credentials": [{
    "type": "password",
    "temporary": false
  }]
}
```

### Atributos Padrão
- **username**: identificador único
- **email**: email do usuário
- **firstName / lastName**: nome completo
- **enabled**: usuário ativo/inativo
- **emailVerified**: email foi verificado?

### Atributos Customizados
Você pode adicionar atributos personalizados:
```
CPF: 123.456.789-00
Cargo: Analista Financeiro
Departamento: Contabilidade
Data Admissão: 2023-01-15
```

### Credentials (Credenciais)
- **Password**: senha do usuário
- **OTP**: senha temporária (TOTP/HOTP)
- **WebAuthn**: autenticação biométrica
- **Temporary**: se a senha é temporária (requer troca no primeiro login)

### Required Actions
Ações que o usuário deve realizar:
- `UPDATE_PASSWORD`: atualizar senha
- `VERIFY_EMAIL`: verificar email
- `UPDATE_PROFILE`: atualizar perfil
- `CONFIGURE_TOTP`: configurar 2FA

---

## Roles

### O que são?
**Roles** são papéis ou permissões que podem ser atribuídas a usuários ou grupos.

### Tipos de Roles

#### 1. **Realm Roles**
Roles globais do realm, aplicáveis a todos os clients:

```
Exemplos:
- admin
- user
- manager
- guest
```

**Quando usar:**
- Roles organizacionais amplas
- Permissões que se aplicam a múltiplas aplicações

#### 2. **Client Roles**
Roles específicas de uma aplicação/client:

```
Client: minha-aplicacao
Roles:
- visualizar_relatorios
- editar_relatorios
- aprovar_transacoes
- gerenciar_usuarios
- exportar_dados
```

**Quando usar:**
- Permissões específicas da aplicação
- Controle granular de acesso

### Composite Roles (Roles Compostas)

Uma role que agrupa outras roles:

```
Role: gerente_financeiro (composite)
└── Contém:
    ├── visualizar_relatorios (client role)
    ├── editar_relatorios (client role)
    ├── aprovar_transacoes (client role)
    ├── user (realm role)
    └── exportar_dados (client role)
```

**Vantagens:**
- Simplifica gestão de permissões
- Atribui múltiplas permissões de uma vez
- Facilita manutenção

### Como Aparecem no Token

```json
{
  "realm_access": {
    "roles": ["user", "manager"]
  },
  "resource_access": {
    "minha-aplicacao": {
      "roles": [
        "visualizar_relatorios",
        "editar_relatorios",
        "aprovar_transacoes"
      ]
    }
  }
}
```

### Atribuição de Roles

#### Direto ao Usuário
```
Users → João Silva → Role Mapping
- Assigned Roles: visualizar_relatorios, user
```

#### Via Grupo
```
Groups → Financeiro → Role Mapping
- Assigned Roles: visualizar_relatorios, editar_relatorios

Users → João Silva → Groups
- Member of: Financeiro (herda as roles)
```

---

## Groups

### O que são?
**Groups** agrupam usuários com características ou permissões comuns.

### Estrutura Hierárquica

```
Groups
├── Empresa
│   ├── Administrativo
│   │   ├── RH
│   │   ├── Financeiro
│   │   └── Jurídico
│   ├── Operacional
│   │   ├── Vendas
│   │   ├── Atendimento
│   │   └── Logística
│   └── TI
│       ├── Desenvolvimento
│       ├── Infraestrutura
│       └── Suporte
```

### Características
- **Hierarquia**: grupos podem ter subgrupos
- **Herança**: subgrupos herdam atributos e roles do grupo pai
- **Atributos**: grupos podem ter atributos customizados
- **Roles**: roles atribuídas ao grupo são herdadas pelos membros

### Exemplo Completo

```yaml
Group: Financeiro
├── Attributes:
│   ├── departamento: "Financeiro"
│   ├── centro_custo: "1001"
│   └── gestor: "maria.santos@exemplo.com.br"
  │
├── Role Mappings:
│   ├── Realm Roles: [user]
│   └── Client Roles (minha-aplicacao):
  │       ├── visualizar_relatorios
  │       ├── editar_relatorios
  │       └── exportar_dados
  │
└── Members:
  ├── joao.silva@exemplo.com.br
  ├── ana.costa@exemplo.com.br
  └── pedro.oliveira@exemplo.com.br
```

### Groups vs Roles

| Aspecto | Groups | Roles |
|---------|--------|-------|
| **Propósito** | Organizar usuários | Definir permissões |
| **Hierarquia** | Suporta (pai/filho) | Não suporta nativamente |
| **Atributos** | Sim | Não |
| **No Token** | Opcional (via mapper) | Sempre presente |
| **Uso típico** | Departamentos, equipes | Permissões, ações |

### Como Aparecem no Token

Grupos precisam de um mapper para aparecer no token:

```json
{
  "groups": [
    "/Empresa/Administrativo/Financeiro",
    "/Empresa/Operacional/Vendas"
  ]
}
```

---

## Client Scopes

### O que são?
**Client Scopes** definem quais informações (claims) serão incluídas nos tokens.

### Tipos

#### 1. **Default Scopes**
Sempre incluídos nos tokens:
```
- profile (nome, email, etc.)
- email
- roles
```

#### 2. **Optional Scopes**
Incluídos apenas se solicitados:
```
- address
- phone
- offline_access (refresh token)
- microprofile-jwt
```

### Mappers

Mappers transformam informações do usuário em claims do token:

#### User Attribute Mapper
```yaml
Name: departamento-mapper
Mapper Type: User Attribute
User Attribute: departamento
Token Claim Name: departamento
Claim JSON Type: String
Add to ID token: ON
Add to access token: ON
```

#### Group Membership Mapper
```yaml
Name: groups-mapper
Mapper Type: Group Membership
Token Claim Name: groups
Full group path: ON
Add to ID token: ON
Add to access token: ON
```

#### Role Mapper
```yaml
Name: realm-roles-mapper
Mapper Type: User Realm Role
Token Claim Name: realm_access.roles
Add to ID token: ON
Add to access token: ON
```

### Exemplo de Token com Scopes

```json
{
  "sub": "a063d40c-df1e-4c99-b291-137f1ca04a21",
  "email_verified": false,
  "name": "João Silva",
  "groups": ["/Financeiro"],
  "preferred_username": "joao.silva",
  "given_name": "João",
  "family_name": "Silva",
  "email": "joao.silva@exemplo.com.br",
  "departamento": "Financeiro",
  "matricula": "12345",
  "realm_access": {
    "roles": ["user", "manager"]
  },
  "resource_access": {
    "minha-aplicacao": {
      "roles": ["visualizar_relatorios"]
    }
  }
}
```

---

## Identity Providers

### O que são?
**Identity Providers (IdP)** permitem autenticação via provedores externos.

### Tipos Suportados

#### Social Providers
- Google
- Facebook
- GitHub
- LinkedIn
- Twitter
- Microsoft

#### Enterprise Providers
- SAML 2.0
- OpenID Connect
- Kerberos

### Exemplo: Google Identity Provider

```yaml
Provider: Google
Client ID: 123456789-abcdefgh.apps.googleusercontent.com
Client Secret: GOCSPX-xxxxxxxxxxxxxxxx
Default Scopes: openid profile email
```

### Fluxo com Identity Provider

```
1. Usuário clica "Login com Google"
   ↓
2. Keycloak redireciona para Google
   ↓
3. Usuário autentica no Google
   ↓
4. Google retorna para Keycloak
   ↓
5. Keycloak cria/atualiza usuário local
   ↓
6. Keycloak emite token próprio
   ↓
7. Aplicação recebe token do Keycloak
```

### Identity Provider Mappers

Mapeiam atributos do provedor externo para atributos locais:

```yaml
Mapper: google-email
Mapper Type: Attribute Importer
Social Profile JSON Field Path: email
User Attribute Name: email
```

---

## User Federation

### O que é?
**User Federation** permite ao Keycloak buscar usuários de fontes externas sem armazená-los.

### Tipos de Federation

#### 1. **LDAP / Active Directory**

```yaml
Vendor: Active Directory
Connection URL: ldap://ad.exemplo.com.br:389
Bind DN: CN=keycloak,CN=Users,DC=empresa,DC=com,DC=br
Bind Credential: senha_secreta
Users DN: CN=Users,DC=empresa,DC=com,DC=br
Username LDAP attribute: sAMAccountName
RDN LDAP attribute: cn
UUID LDAP attribute: objectGUID
```

**Fluxo:**
```
1. Usuário digita username/password
   ↓
2. Keycloak consulta LDAP
   ↓
3. LDAP valida credenciais
   ↓
4. Keycloak cria sessão local
   ↓
5. Usuário recebe token
```

#### 2. **Kerberos**

Para integração com ambientes Windows:

```yaml
Kerberos Realm: EMPRESA.COM.BR
Server Principal: HTTP/keycloak.exemplo.com.br@EMPRESA.COM.BR
KeyTab: /path/to/keycloak.keytab
```

#### 3. **Custom User Storage SPI**

Para banco de dados customizado:

```java
public class CustomUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator {

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        // Buscar no banco de dados externo
        User user = externalDatabase.findByUsername(username);
        return new UserAdapter(session, realm, model, user);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        // Validar senha no sistema externo
        return externalDatabase.validatePassword(user.getUsername(), input.getValue());
    }
}
```

### Sincronização

User Federation pode sincronizar usuários:

```yaml
Import Users: ON (importa para Keycloak)
Sync Registrations: OFF (não envia novos usuários para LDAP)
Periodic Full Sync: ON (sincroniza todos diariamente)
Periodic Changed Users Sync: ON (sincroniza mudanças a cada hora)
```

---

## Authentication Flows

### O que são?
**Authentication Flows** definem como o usuário será autenticado (senha, OTP, biometria, etc.).

### Flows Padrão

#### 1. **Browser Flow**
Para login via navegador web:

```
Browser Flow
├── Cookie (check session)
├── Identity Provider Redirector
├── Forms
│   ├── Username Password Form
│   └── OTP Form (condicional)
└── Conditional OTP
```

#### 2. **Direct Grant Flow**
Para APIs (Resource Owner Password Credentials):

```
Direct Grant Flow
├── Username Validation
├── Password
└── OTP (condicional)
```

#### 3. **Registration Flow**
Para auto-registro de usuários:

```
Registration Flow
├── Profile Validation
├── Password Validation
├── reCAPTCHA
└── Terms and Conditions
```

### Executors

Cada step do flow é um executor:

- **REQUIRED**: obrigatório, falha = autenticação falha
- **ALTERNATIVE**: pelo menos um alternative deve passar
- **DISABLED**: desabilitado
- **CONDITIONAL**: executa baseado em condição

### Exemplo: Flow com 2FA

```
Custom Browser Flow
├── Cookie [ALTERNATIVE]
├── Identity Provider Redirector [ALTERNATIVE]
└── Forms [ALTERNATIVE]
    ├── Username Password Form [REQUIRED]
    └── Conditional OTP [CONDITIONAL]
        ├── Condition - User Configured [REQUIRED]
        └── OTP Form [REQUIRED]
```

**Comportamento:**
1. Se tem cookie válido → autenticado
2. Senão, mostra formulário de login
3. Após login, se usuário tem OTP configurado → solicita código
4. Se não tem OTP configurado → autenticado direto

---

## Tokens e Sessões

### Tipos de Tokens

#### 1. **Access Token**
- Token JWT usado para acessar recursos protegidos
- Curta duração (5-15 minutos típico)
- Contém claims do usuário e roles

```json
{
  "exp": 1705942500,
  "iat": 1705942200,
  "jti": "a8f3d9e2-4c7b-11ed-bdc3-0242ac120002",
  "iss": "https://keycloak.exemplo.com.br/realms/minha-aplicacao",
  "aud": ["minha-aplicacao"],
  "sub": "a063d40c-df1e-4c99-b291-137f1ca04a21",
  "typ": "Bearer",
  "azp": "minha-aplicacao",
  "session_state": "5911d241-b395-44db-8748-66c2c3b5838b",
  "realm_access": {
    "roles": ["user"]
  },
  "resource_access": {
    "minha-aplicacao": {
      "roles": ["visualizar_relatorios"]
    }
  },
  "scope": "openid email profile",
  "email_verified": false,
  "name": "João Silva",
  "preferred_username": "joao.silva",
  "given_name": "João",
  "family_name": "Silva",
  "email": "joao.silva@exemplo.com.br"
}
```

#### 2. **Refresh Token**
- Token usado para obter novos access tokens
- Longa duração (30 minutos - dias)
- Não contém informações do usuário

```json
{
  "exp": 1705944000,
  "iat": 1705942200,
  "jti": "b9e4f0a3-5d8c-22fe-cde4-1353bd231003",
  "iss": "https://keycloak.exemplo.com.br/realms/minha-aplicacao",
  "aud": "https://keycloak.exemplo.com.br/realms/minha-aplicacao",
  "sub": "a063d40c-df1e-4c99-b291-137f1ca04a21",
  "typ": "Refresh",
  "azp": "minha-aplicacao",
  "session_state": "5911d241-b395-44db-8748-66c2c3b5838b",
  "scope": "openid email profile"
}
```

#### 3. **ID Token**
- Token OpenID Connect com informações do usuário
- Usado pelo client para identificar o usuário
- Mesma duração do access token

```json
{
  "exp": 1705942500,
  "iat": 1705942200,
  "auth_time": 1705942200,
  "jti": "c0f5g1b4-6e9d-33gf-def5-2464ce342114",
  "iss": "https://keycloak.exemplo.com.br/realms/minha-aplicacao",
  "aud": "minha-aplicacao",
  "sub": "a063d40c-df1e-4c99-b291-137f1ca04a21",
  "typ": "ID",
  "azp": "minha-aplicacao",
  "session_state": "5911d241-b395-44db-8748-66c2c3b5838b",
  "at_hash": "RJLZKz3xyE8LkV1aLkx8Xg",
  "acr": "1",
  "email_verified": false,
  "name": "João Silva",
  "preferred_username": "joao.silva",
  "given_name": "João",
  "family_name": "Silva",
  "email": "joao.silva@exemplo.com.br"
}
```

### Configuração de Tempo de Vida

```yaml
Realm Settings → Tokens

Access Token Lifespan: 5 minutes
Access Token Lifespan For Implicit Flow: 15 minutes
Client Login Timeout: 1 minute
Login Timeout: 30 minutes
Login Action Timeout: 5 minutes

SSO Session Idle: 30 minutes
SSO Session Max: 10 hours
SSO Session Idle Remember Me: 30 days
SSO Session Max Remember Me: 30 days

Offline Session Idle: 30 days
Offline Session Max: 60 days
```

### Renovação de Token

```bash
# Renovar access token usando refresh token
curl --location 'https://keycloak.exemplo.com.br/realms/minha-aplicacao/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=refresh_token' \
--data-urlencode 'client_id=minha-aplicacao' \
--data-urlencode 'refresh_token=eyJhbGciOiJIUzUxMiIsInR5cCI...'
```

### Sessões

#### User Session
- Criada no login
- Vinculada aos tokens
- Pode ser revogada pelo admin
- Timeout configurável

#### Client Session
- Uma por aplicação acessada
- Criada quando client solicita token
- Permite SSO entre múltiplos clients

### SSO (Single Sign-On)

```
Usuário já autenticado no Keycloak
├── Acessa App 1 → Token emitido (sem novo login)
├── Acessa App 2 → Token emitido (sem novo login)
└── Acessa App 3 → Token emitido (sem novo login)
```

### Logout

#### Single Logout
```
POST /realms/minha-aplicacao/protocol/openid-connect/logout
```

Invalida sessão do usuário em todas as aplicações.

---

## Fluxo Completo de Autenticação

### Authorization Code Flow (Recomendado para Web)

```mermaid
sequenceDiagram
    participant User as Usuário
    participant App as Aplicação
    participant KC as Keycloak
    participant API as API Backend

    User->>App: 1. Acessa aplicação
    App->>KC: 2. Redirect para /auth
    Note over KC: client_id, redirect_uri, scope
    
    KC->>User: 3. Mostra tela de login
    User->>KC: 4. Digita credenciais
    
    KC->>KC: 5. Valida credenciais
    KC->>App: 6. Redirect com code
    Note over App: ?code=abc123&session_state=xyz
    
    App->>KC: 7. POST /token com code
    Note over KC: Troca code por tokens
    KC->>App: 8. Retorna access_token, refresh_token, id_token
    
    App->>API: 9. Request com Authorization: Bearer {access_token}
    API->>KC: 10. Valida token (JWKS)
    KC->>API: 11. Token válido + claims
    API->>App: 12. Resposta com dados
    App->>User: 13. Mostra conteúdo
```

### 1. Iniciar Autenticação

```http
GET /realms/minha-aplicacao/protocol/openid-connect/auth?
    response_type=code&
    client_id=minha-aplicacao&
    redirect_uri=https://bi.exemplo.com.br/callback&
    scope=openid email profile&
    state=random_state_value
```

**Parâmetros:**
- `response_type=code`: solicita authorization code
- `client_id`: identificador da aplicação
- `redirect_uri`: onde Keycloak redirecionará
- `scope`: quais informações solicitar
- `state`: proteção contra CSRF

### 2. Usuário Autentica

Keycloak mostra tela de login. Usuário digita credenciais.

### 3. Keycloak Retorna Code

```http
HTTP/1.1 302 Found
Location: https://bi.exemplo.com.br/callback?
    code=c1168df4-d9f0-4404-a3d1-9022c71c37bd.5911d241-b395-44db-8748-66c2c3b5838b.97785ccc-e9b7-4a2f-8ca9-a3a992cb4e20&
    session_state=5911d241-b395-44db-8748-66c2c3b5838b&
    state=random_state_value
```

### 4. Aplicação Troca Code por Tokens

```bash
curl --location 'https://keycloak.exemplo.com.br/realms/minha-aplicacao/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=authorization_code' \
--data-urlencode 'code=c1168df4-d9f0-4404-a3d1-9022c71c37bd...' \
--data-urlencode 'client_id=minha-aplicacao' \
--data-urlencode 'redirect_uri=https://bi.exemplo.com.br/callback'
```

### 5. Keycloak Retorna Tokens

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzUxMiIsInR5cCI...",
  "token_type": "Bearer",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "not-before-policy": 0,
  "session_state": "5911d241-b395-44db-8748-66c2c3b5838b",
  "scope": "openid email profile"
}
```

### 6. Aplicação Usa Access Token

```bash
curl --location 'https://api.exemplo.com.br/relatorios' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI...'
```

### 7. API Valida Token

A API valida o token JWT usando a chave pública do Keycloak:

```http
GET /realms/minha-aplicacao/protocol/openid-connect/certs
```

Retorna JWKS (JSON Web Key Set) para validação:

```json
{
  "keys": [
    {
      "kid": "FJ86GcF3jTbNLOco4NvZkUCIUmfYCqoqtOQeMfbhNlE",
      "kty": "RSA",
      "alg": "RS256",
      "use": "sig",
      "n": "xGOr-H7A...",
      "e": "AQAB"
    }
  ]
}
```

### 8. Renovar Token Expirado

```bash
curl --location 'https://keycloak.exemplo.com.br/realms/minha-aplicacao/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=refresh_token' \
--data-urlencode 'client_id=minha-aplicacao' \
--data-urlencode 'refresh_token=eyJhbGciOiJIUzUxMiIsInR5cCI...'
```

### 9. Obter Informações do Usuário

```bash
curl --location 'https://keycloak.exemplo.com.br/realms/minha-aplicacao/protocol/openid-connect/userinfo' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI...'
```

Resposta:

```json
{
  "sub": "a063d40c-df1e-4c99-b291-137f1ca04a21",
  "email_verified": false,
  "name": "João Silva",
  "preferred_username": "joao.silva",
  "given_name": "João",
  "family_name": "Silva",
  "email": "joao.silva@exemplo.com.br"
}
```

### 10. Logout

```bash
curl --location --request POST 'https://keycloak.exemplo.com.br/realms/minha-aplicacao/protocol/openid-connect/logout' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'client_id=minha-aplicacao' \
--data-urlencode 'refresh_token=eyJhbGciOiJIUzUxMiIsInR5cCI...'
```

---

## Resumo das Entidades

| Entidade | Propósito | Exemplo |
|----------|-----------|---------|
| **Realm** | Domínio isolado | `minha-aplicacao` |
| **Client** | Aplicação que usa Keycloak | `minha-aplicacao-frontend` |
| **User** | Conta de usuário | `joao.silva@exemplo.com.br` |
| **Role** | Permissão/papel | `visualizar_relatorios` |
| **Group** | Agrupamento de usuários | `Financeiro` |
| **Client Scope** | Claims incluídos no token | `profile`, `email` |
| **Identity Provider** | Autenticação externa | Google, SAML |
| **User Federation** | Fonte externa de usuários | LDAP, Active Directory |
| **Authentication Flow** | Como autenticar | Browser Flow, Direct Grant |

---

## Boas Práticas

### Segurança
1. Use HTTPS sempre
2. Configure valid redirect URIs restritivamente
3. Use client secrets fortes (confidential clients)
4. Habilite brute force detection
5. Configure timeouts adequados
6. Use refresh tokens com rotação

### Organização
1. Um realm por ambiente/organização
2. Client roles para permissões específicas da aplicação
3. Realm roles para permissões organizacionais
4. Groups para organizar usuários
5. Composite roles para simplificar atribuições

### Performance
1. Cache JWKS na aplicação
2. Use client scopes para reduzir tamanho do token
3. Configure timeouts apropriados
4. Use sessões offline apenas quando necessário

### Manutenção
1. Documente mappers customizados
2. Use nomes descritivos para roles/groups
3. Revise periodicamente usuários inativos
4. Monitore logs de autenticação
5. Teste flows customizados antes de aplicar

---

## Recursos Adicionais

- **Documentação Oficial**: https://www.keycloak.org/documentation
- **Admin Console**: https://keycloak.exemplo.com.br/admin
- **Account Console**: https://keycloak.exemplo.com.br/realms/minha-aplicacao/account
- **API Endpoints**: https://keycloak.exemplo.com.br/realms/minha-aplicacao/.well-known/openid-configuration

---

**Versão do Documento**: 1.0  
**Data**: Janeiro 2026  
**Autor**: Documentação Keycloak - EMPRESA
