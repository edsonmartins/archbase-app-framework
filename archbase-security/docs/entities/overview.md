[← Voltar ao Índice](../README.md) | [Próximo: Entidades Core →](core-entities.md)

---

# Visão Geral das Entidades de Segurança

## Introdução

Este documento apresenta uma visão geral da estrutura de entidades do módulo `archbase-security`. Todas as entidades seguem os princípios de Domain-Driven Design (DDD) e suportam arquiteturas multi-tenant desde o design.

## Hierarquia de Entidades

### SecurityEntity (Classe Base Abstrata)

**Localização**: `br.com.archbase.security.persistence.SecurityEntity`

SecurityEntity é a classe base abstrata para todas as entidades de segurança no sistema. Utiliza a estratégia Single Table Inheritance (STI) do JPA, onde todas as subclasses são armazenadas em uma única tabela `SEGURANCA`.

#### Estratégia de Herança

```java
@Entity
@Table(name="SEGURANCA")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="TP_SEGURANCA")
```

Essa estratégia permite que múltiplos tipos de entidades de segurança (User, Group, Profile) sejam armazenados na mesma tabela, diferenciados pela coluna discriminadora `TP_SEGURANCA`.

**Vantagens**:
- Consultas polimórficas simples (buscar todos os tipos de segurança)
- Melhor performance para queries que envolvem múltiplos tipos
- Facilita relacionamentos com `PermissionEntity`

**Desvantagens**:
- Tabela pode ficar grande
- Colunas específicas de subclasses podem ter muitos `NULL`

#### Campos Comuns

Todos os tipos de entidades de segurança herdam os seguintes campos:

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_SEGURANCA | UUID | Identificador único |
| `code` | CD_SEGURANCA | String | Código de negócio |
| `name` | NOME | String | Nome da entidade de segurança |
| `description` | DESCRICAO | String | Descrição detalhada |
| `tenantId` | TENANT_ID | String | Identificador do tenant (multi-tenancy) |
| `version` | VERSION | Long | Controle de versionamento otimista |
| `createEntityDate` | CREATE_DATE | LocalDateTime | Data de criação |
| `createdByUser` | CREATED_BY | String | Usuário criador |
| `updateEntityDate` | UPDATE_DATE | LocalDateTime | Data da última atualização |
| `lastModifiedByUser` | LAST_MODIFIED_BY | String | Último usuário que modificou |

**Nota sobre Multi-Tenancy**: O campo `tenantId` é herdado de `TenantPersistenceEntityBase`, garantindo isolamento de dados entre diferentes organizações.

#### Subclasses

SecurityEntity possui três subclasses principais:

1. **UserEntity** (discriminador: `"USUARIO"`)
   - Representa usuários individuais do sistema
   - Implementa `UserDetails` do Spring Security
   - Contém credenciais, políticas de senha e configurações de acesso

2. **GroupEntity** (discriminador: `"SEGURANCA_GRUPO"`)
   - Representa agrupamentos lógicos de usuários
   - Usado para organização estrutural (departamentos, equipes)
   - Facilita atribuição de permissões em massa

3. **ProfileEntity** (discriminador: `"SEGURANCA_PERFIL"`)
   - Representa papéis funcionais no sistema
   - Define conjuntos predefinidos de permissões
   - Usado para controle de acesso baseado em função (RBAC)

## Categorias de Entidades

As entidades do módulo de segurança estão organizadas em quatro categorias principais:

### 1. Entidades Core
- [SecurityEntity](core-entities.md#securityentity)
- [UserEntity](core-entities.md#userentity)
- [GroupEntity](core-entities.md#groupentity)
- [ProfileEntity](core-entities.md#profileentity)
- [AccessScheduleEntity](core-entities.md#accessscheduleentity)
- [AccessIntervalEntity](core-entities.md#accessintervalentity)

### 2. Sistema de Permissões
- [PermissionEntity](permission-entities.md#permissionentity)
- [ResourceEntity](permission-entities.md#resourceentity)
- [ActionEntity](permission-entities.md#actionentity)

### 3. Gerenciamento de Tokens
- [AccessTokenEntity](token-entities.md#accesstokenentity)
- [ApiTokenEntity](token-entities.md#apitokenentity)
- [PasswordResetTokenEntity](token-entities.md#passwordresettokenentity)

### 4. Relacionamentos
- **UserGroupEntity**: Relacionamento many-to-many entre User e Group

## Mapa de Relacionamentos

```
SecurityEntity (Base Abstrata)
│
├── UserEntity
│   ├── → ProfileEntity (ManyToOne)
│   ├── → AccessScheduleEntity (ManyToOne)
│   ├── ← UserGroupEntity (OneToMany)
│   ├── ← AccessTokenEntity (OneToMany)
│   ├── ← ApiTokenEntity (OneToMany)
│   └── ← PasswordResetTokenEntity (OneToMany)
│
├── GroupEntity
│   └── ← UserGroupEntity (OneToMany)
│
└── ProfileEntity
    └── ← UserEntity (OneToMany)

PermissionEntity
├── → SecurityEntity (ManyToOne) - user/group/profile
└── → ActionEntity (ManyToOne)

ResourceEntity
└── ← ActionEntity (OneToMany)

AccessScheduleEntity
└── ← AccessIntervalEntity (OneToMany)
```

## Princípios de Design

### 1. Separação de Responsabilidades
- **Entidades de Persistência** (`persistence` package): Mapeamento JPA e acesso ao banco
- **Entidades de Domínio** (`domain.entity` package): Lógica de negócio
- **DTOs** (`domain.dto` package): Transferência de dados para clientes

### 2. Multi-Tenancy desde o Design
- Todas as entidades principais estendem `TenantPersistenceEntityBase`
- Filtragem automática por `tenantId`
- Suporte para três níveis de isolamento (tenant, company, project)

### 3. Auditabilidade
- Todos os campos de auditoria (`createEntityDate`, `createdByUser`, etc.) são herdados
- Versionamento otimista com `@Version`
- Rastreamento completo de modificações

### 4. Flexibilidade Contextual
- Permissões podem ser globais ou restritas a contextos específicos
- Suporte para wildcards (`null` = qualquer valor)
- Escopos hierárquicos (global → tenant → company → project)

## Próximos Passos

- [Entidades Core](core-entities.md) - Detalhes sobre User, Group, Profile e controle temporal
- [Sistema de Permissões](permission-entities.md) - Como Permission, Resource e Action implementam RBAC
- [Gerenciamento de Tokens](token-entities.md) - Access tokens, API tokens e redefinição de senha

---

[← Voltar ao Índice](../README.md) | [Próximo: Entidades Core →](core-entities.md)
