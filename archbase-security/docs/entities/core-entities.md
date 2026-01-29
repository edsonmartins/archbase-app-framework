[← Anterior: Visão Geral](overview.md) | [Voltar ao Índice](../README.md) | [Próximo: Sistema de Permissões →](permission-entities.md)

---

# Entidades Core do Sistema de Segurança

## 1. SecurityEntity (Classe Base)

**Localização**: `br.com.archbase.security.persistence.SecurityEntity`
**Tabela**: `SEGURANCA`

SecurityEntity é a classe base abstrata para todas as entidades de segurança no sistema. Implementa a estratégia Single Table Inheritance (STI), onde todas as subclasses compartilham uma única tabela.

### Configuração JPA

```java
@Entity
@Table(name="SEGURANCA")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="TP_SEGURANCA")
public abstract class SecurityEntity extends TenantPersistenceEntityBase<SecurityEntity, UUID> {
    // Campos e métodos
}
```

### Campos Principais

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_SEGURANCA | UUID | Identificador único |
| `code` | CD_SEGURANCA | String | Código de negócio único |
| `name` | NOME | String(255) | Nome da entidade |
| `description` | DESCRICAO | String(500) | Descrição detalhada |

### Campos Herdados de TenantPersistenceEntityBase

| Campo | Descrição |
|-------|-----------|
| `tenantId` | Identificador do tenant (isolamento multi-tenant) |
| `version` | Versionamento otimista (controle de concorrência) |
| `createEntityDate` | Data/hora de criação |
| `createdByUser` | Usuário que criou a entidade |
| `updateEntityDate` | Data/hora da última modificação |
| `lastModifiedByUser` | Usuário que fez a última modificação |

### Subclasses

- **UserEntity**: discriminador `"USUARIO"`
- **GroupEntity**: discriminador `"SEGURANCA_GRUPO"`
- **ProfileEntity**: discriminador `"SEGURANCA_PERFIL"`

---

## 2. UserEntity

**Localização**: `br.com.archbase.security.persistence.UserEntity`
**Tabela**: `SEGURANCA` (TP_SEGURANCA = 'USUARIO')

Representa um usuário no sistema, implementando `UserDetails` do Spring Security para integração nativa com o framework de segurança.

### Campos de Credenciais

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `userName` | USER_NAME | String(150) | Nome de usuário para login (único por tenant) |
| `password` | SENHA | String(500) | Senha criptografada (BCrypt) |
| `email` | EMAIL | String(150) | Endereço de email (único por tenant) |
| `nickname` | APELIDO | String(150) | Nome de exibição amigável |

**Constraints Únicos**:
- `(TENANT_ID, USER_NAME)` - Nome de usuário único dentro do tenant
- `(TENANT_ID, EMAIL)` - Email único dentro do tenant

### Campos de Controle de Segurança

| Campo | Coluna | Tipo | Padrão | Descrição |
|-------|--------|------|--------|-----------|
| `isAdministrator` | BO_ADMINISTRADOR | Boolean | false | Se `true`, bypassa verificação de permissões |
| `accountDeactivated` | BO_CONTA_DESATIVADA | Boolean | false | Conta desativada (login negado) |
| `accountLocked` | CONTA_BLOQUEADA | Boolean | false | Conta bloqueada (login negado) |

**Importante**: Administradores só têm bypass se `isAdministrator=true` **E** `isEnabled()=true`.

### Políticas de Senha

| Campo | Coluna | Tipo | Padrão | Descrição |
|-------|--------|------|--------|-----------|
| `changePasswordOnNextLogin` | BO_ALTERAR_SENHA_PROXIMO_LOGIN | Boolean | false | Força alteração de senha no próximo login |
| `allowPasswordChange` | BO_PERMITE_ALTERAR_SENHA | Boolean | true | Permite que o usuário altere sua própria senha |
| `passwordNeverExpires` | BO_SENHA_NUNCA_EXPIRA | Boolean | false | Se `true`, senha não expira |

**Uso Típico**:
- Novo usuário: `changePasswordOnNextLogin=true` para forçar definição de senha própria
- Contas de serviço: `allowPasswordChange=false` para evitar alterações acidentais
- Administradores: `passwordNeverExpires=true` (opcional, dependendo da política)

### Controle de Acesso

| Campo | Coluna | Tipo | Padrão | Descrição |
|-------|--------|------|--------|-----------|
| `allowMultipleLogins` | BO_PERMITE_MULTIPLICOS_LOGINS | Boolean | false | Permite múltiplas sessões simultâneas |
| `unlimitedAccessHours` | BO_HORARIO_LIVRE | Boolean | true | Acesso ilimitado sem restrição de horário |
| `avatar` | AVATAR | BLOB | null | Imagem do avatar do usuário |

**Comportamento de `allowMultipleLogins`**:
- `false`: Ao fazer login, todos os tokens anteriores são revogados
- `true`: Múltiplos tokens podem existir simultaneamente (múltiplos dispositivos)

### Relacionamentos

#### ManyToOne

| Relacionamento | Entidade | Descrição |
|----------------|----------|-----------|
| `profile` | ProfileEntity | Perfil principal do usuário (define permissões padrão) |
| `accessSchedule` | AccessScheduleEntity | Horários permitidos de acesso (se `unlimitedAccessHours=false`) |

#### OneToMany

| Relacionamento | Entidade | Descrição |
|----------------|----------|-----------|
| `groups` | UserGroupEntity | Grupos aos quais o usuário pertence |
| `tokens` | AccessTokenEntity | Tokens JWT ativos do usuário |
| `apiTokens` | ApiTokenEntity | Tokens de API do usuário |
| `passwordResetTokens` | PasswordResetTokenEntity | Tokens de redefinição de senha |

### Integração com Spring Security

UserEntity implementa `org.springframework.security.core.userdetails.UserDetails`, fornecendo os métodos exigidos pelo Spring Security:

```java
@Override
public String getUsername() {
    return this.userName;  // ou this.email, dependendo da configuração
}

@Override
public boolean isEnabled() {
    return !this.accountDeactivated && !this.accountLocked;
}

@Override
public boolean isAccountNonExpired() {
    return !this.accountDeactivated;
}

@Override
public boolean isAccountNonLocked() {
    return !this.accountLocked;
}

@Override
public boolean isCredentialsNonExpired() {
    return this.passwordNeverExpires;
}

@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    // Retorna permissões do usuário (via profile e grupos)
    // Implementação específica no service
}
```

**Importante**: O método `isEnabled()` é usado na verificação de bypass de administrador. Mesmo administradores com contas desativadas ou bloqueadas **não** terão acesso.

---

## 3. GroupEntity

**Localização**: `br.com.archbase.security.persistence.GroupEntity`
**Tabela**: `SEGURANCA` (TP_SEGURANCA = 'SEGURANCA_GRUPO')

Representa um agrupamento lógico de usuários, facilitando a atribuição de permissões a múltiplos usuários simultaneamente.

### Campos

GroupEntity **não possui campos específicos** além dos herdados de SecurityEntity:
- `id`, `code`, `name`, `description`
- `tenantId`, campos de auditoria

### Relacionamentos

| Relacionamento | Entidade | Descrição |
|----------------|----------|-----------|
| `users` | UserGroupEntity | Usuários que pertencem ao grupo |
| `permissions` | PermissionEntity | Permissões atribuídas ao grupo |

### Uso Típico

Grupos são usados para organização estrutural de usuários:

**Exemplos**:
- "Equipe de Vendas"
- "Desenvolvedores Backend"
- "Gerentes de Projeto"
- "Suporte Técnico Nível 1"

**Caso de Uso**:
```java
// Criar grupo
GroupEntity salesTeam = GroupEntity.builder()
    .code("SALES_TEAM")
    .name("Equipe de Vendas")
    .description("Todos os vendedores da empresa")
    .build();

// Atribuir permissão ao grupo
PermissionEntity permission = PermissionEntity.builder()
    .security(salesTeam)
    .action(viewCustomersAction)
    .build();

// Todos os usuários do grupo herdam a permissão automaticamente
```

---

## 4. ProfileEntity

**Localização**: `br.com.archbase.security.persistence.ProfileEntity`
**Tabela**: `SEGURANCA` (TP_SEGURANCA = 'SEGURANCA_PERFIL')

Representa um perfil ou papel no sistema, definindo um conjunto predefinido de permissões e comportamentos.

### Campos

ProfileEntity **não possui campos específicos** além dos herdados de SecurityEntity:
- `id`, `code`, `name`, `description`
- `tenantId`, campos de auditoria

### Relacionamentos

| Relacionamento | Entidade | Descrição |
|----------------|----------|-----------|
| `users` | UserEntity | Usuários que possuem este perfil |
| `permissions` | PermissionEntity | Permissões atribuídas ao perfil |

### Uso Típico

Perfis são usados para definir papéis funcionais com conjuntos específicos de permissões:

**Exemplos**:
- "Administrador do Sistema"
- "Operador"
- "Consultor"
- "Auditor"

**Caso de Uso**:
```java
// Criar perfil
ProfileEntity adminProfile = ProfileEntity.builder()
    .code("ADMIN")
    .name("Administrador")
    .description("Acesso completo ao sistema")
    .build();

// Atribuir múltiplas permissões ao perfil
List<PermissionEntity> permissions = Arrays.asList(
    createPermission(adminProfile, viewUsersAction),
    createPermission(adminProfile, createUsersAction),
    createPermission(adminProfile, editUsersAction),
    createPermission(adminProfile, deleteUsersAction)
);

// Usuário recebe o perfil
user.setProfile(adminProfile);
// Agora o usuário herda todas as permissões do perfil
```

### Diferença entre Group e Profile

| Aspecto | Group | Profile |
|---------|-------|---------|
| **Propósito** | Agrupamento organizacional | Papel funcional |
| **Base** | Estrutura da organização | Função/responsabilidade |
| **Exemplo** | "Equipe de Marketing" | "Gerente" |
| **Permissões** | Específicas para o grupo | Definem o papel |
| **Cardinalidade** | Usuário pode ter múltiplos grupos | Usuário tem 1 perfil principal |
| **Uso** | Colaboração, comunicação | Controle de acesso |

**Recomendação**: Use **Profile** para controle de acesso baseado em função (RBAC) e **Group** para organização estrutural e colaboração.

---

## 5. AccessScheduleEntity

**Localização**: `br.com.archbase.security.persistence.AccessScheduleEntity`
**Tabela**: `SEGURANCA_HORARIO_ACESSO`

Define horários permitidos de acesso para usuários, permitindo restrição temporal de login.

### Campos

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_HORARIO_ACESSO | UUID | Identificador único |
| `description` | DESCRICAO | String(500) | Descrição do horário de acesso |

### Relacionamentos

| Relacionamento | Entidade | Descrição |
|----------------|----------|-----------|
| `intervals` | AccessIntervalEntity | Intervalos de tempo específicos (OneToMany) |
| `users` | UserEntity | Usuários que usam este horário (OneToMany) |

### Uso

AccessSchedule é usado para restringir acesso de usuários a janelas de tempo específicas:

**Casos de Uso**:
- Horário comercial: segunda a sexta, 8h às 18h
- Plantão de fim de semana: sábado e domingo, 9h às 13h
- Janela de manutenção: terça-feira, 2h às 5h (para administradores)

**Exemplo**:
```java
AccessScheduleEntity businessHours = AccessScheduleEntity.builder()
    .description("Horário Comercial")
    .build();

// Associar ao usuário
user.setAccessSchedule(businessHours);
user.setUnlimitedAccessHours(false);  // Importante!
```

**Importante**: O campo `UserEntity.unlimitedAccessHours` deve ser `false` para que o AccessSchedule seja validado.

---

## 6. AccessIntervalEntity

**Localização**: `br.com.archbase.security.persistence.AccessIntervalEntity`
**Tabela**: `SEGURANCA_INTERVALO_ACESSO`

Define um intervalo específico de tempo dentro de um horário de acesso.

### Campos

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_INTERVALO_ACESSO | UUID | Identificador único |
| `dayOfWeek` | DIA_DA_SEMANA | Integer | Dia da semana (1=Domingo, 7=Sábado) |
| `startTime` | HORA_INICIAL | String | Hora de início (formato: "HH:mm") |
| `endTime` | HORA_FINAL | String | Hora de término (formato: "HH:mm") |

### Relacionamentos

| Relacionamento | Entidade | Descrição |
|----------------|----------|-----------|
| `accessSchedule` | AccessScheduleEntity | Horário de acesso ao qual pertence (ManyToOne) |

### Formato de Horários

- **dayOfWeek**: `1` (Domingo) até `7` (Sábado)
  - Nota: Seguindo `java.time.DayOfWeek`, onde `1 = SUNDAY`
- **startTime/endTime**: String no formato `"HH:mm"` (24 horas)
  - Exemplo: `"08:00"`, `"18:30"`

### Exemplo Completo

```java
// Criar horário de acesso
AccessScheduleEntity businessHours = AccessScheduleEntity.builder()
    .description("Horário Comercial (Seg-Sex, 8h-18h)")
    .build();
accessScheduleRepository.save(businessHours);

// Criar intervalos (segunda a sexta)
for (int day = 2; day <= 6; day++) {  // 2=Monday, 6=Friday
    AccessIntervalEntity interval = AccessIntervalEntity.builder()
        .accessSchedule(businessHours)
        .dayOfWeek(day)
        .startTime("08:00")
        .endTime("18:00")
        .build();
    accessIntervalRepository.save(interval);
}

// Associar ao usuário
user.setAccessSchedule(businessHours);
user.setUnlimitedAccessHours(false);
userRepository.save(user);
```

**Validação**: Durante autenticação, o sistema verifica:
1. Se `user.unlimitedAccessHours == false`
2. Se hora atual está dentro de algum `AccessIntervalEntity` do dia da semana atual
3. Login é negado se fora dos intervalos permitidos

---

## 7. UserGroupEntity (Relacionamento)

**Localização**: `br.com.archbase.security.persistence.UserGroupEntity`
**Tabela**: `SEGURANCA_GRUPO_USUARIO`

Tabela de junção para relacionamento many-to-many entre User e Group.

### Campos

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_GRUPO_USUARIO | UUID | Identificador único |
| `user` | ID_USUARIO | UUID (FK) | Referência ao usuário |
| `group` | ID_GRUPO | UUID (FK) | Referência ao grupo |

### Constraint Único

`(ID_USUARIO, ID_GRUPO)` - Um usuário não pode estar no mesmo grupo duas vezes.

### Uso

```java
// Adicionar usuário a um grupo
UserGroupEntity userGroup = UserGroupEntity.builder()
    .user(user)
    .group(salesTeam)
    .build();
userGroupRepository.save(userGroup);

// Agora o usuário herda permissões do grupo "salesTeam"
```

---

## Resumo das Entidades Core

| Entidade | Propósito | Campos Específicos | Uso Principal |
|----------|-----------|-------------------|---------------|
| **SecurityEntity** | Classe base abstrata | Nenhum (abstrata) | Herança polimórfica |
| **UserEntity** | Usuários individuais | Credenciais, políticas de senha, controle de acesso | Autenticação e autorização |
| **GroupEntity** | Agrupamentos de usuários | Nenhum | Organização estrutural |
| **ProfileEntity** | Papéis funcionais | Nenhum | Controle de acesso por função (RBAC) |
| **AccessScheduleEntity** | Horários de acesso | Descrição | Restrição temporal de acesso |
| **AccessIntervalEntity** | Intervalos de tempo | Dia, hora inicial, hora final | Definição de janelas de acesso |
| **UserGroupEntity** | Relacionamento User-Group | User ID, Group ID | Associação many-to-many |

---

**Ver também**:
- [Sistema de Permissões](permission-entities.md) - Permission, Resource, Action
- [Gerenciamento de Tokens](token-entities.md) - Access tokens e API tokens
- [Fluxo de Autenticação](../architecture/authentication-system.md) - Como usuários fazem login
- [Multi-Tenancy](../architecture/multi-tenancy.md) - Isolamento de dados por tenant

---

[← Anterior: Visão Geral](overview.md) | [Voltar ao Índice](../README.md) | [Próximo: Sistema de Permissões →](permission-entities.md)
