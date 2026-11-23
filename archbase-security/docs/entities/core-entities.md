[← Anterior: Visão Geral](overview.md) | [Voltar ao Índice](../README.md) | [Próximo: Sistema de Permissões →](permission-entities.md)

---

# Entidades Core do Sistema de Segurança

Colunas conferidas contra `persistence/*.java` na versão 3.5.0. As colunas herdadas da base
(`VERSAO`, `DH_CRIACAO`, `USUARIO_CRIOU`, `DH_ATUALIZACAO`, `ULTIMO_USUARIO_ALTEROU`, `TENANT_ID`)
estão em [Visão Geral](overview.md#colunas-herdadas) e não se repetem abaixo. "Nulável" é o que o
mapeamento declara: `@Column` sem `nullable = false` é nulável.

## SecurityEntity

**Classe**: `br.com.archbase.security.persistence.SecurityEntity` (abstrata)
**Tabela**: `SEGURANCA`

```java
@org.hibernate.envers.Audited
@Entity
@Table(name="SEGURANCA")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="TP_SEGURANCA")
@AttributeOverrides({
        @AttributeOverride(name="id",   column=@Column(name="ID_SEGURANCA", length = 40)),
        @AttributeOverride(name="code", column=@Column(name="CD_SEGURANCA", length = 40))
})
public abstract class SecurityEntity extends TenantPersistenceEntityBase { ... }
```

`TenantPersistenceEntityBase` não é genérica: não existe `TenantPersistenceEntityBase<SecurityEntity, UUID>`.

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_SEGURANCA` (40) | `String` | não (PK) | |
| `code` | `CD_SEGURANCA` (40) | `String` | sim | |
| (discriminador) | `TP_SEGURANCA` | — | não | `USUARIO`, `SEGURANCA_GRUPO` ou `SEGURANCA_PERFIL`. |
| `name` | `NOME` | `String` | **não** | `@NotBlank`. Vale para as três subclasses. |
| `description` | `DESCRICAO` | `String` | **não** | `@NotBlank`. Vale para as três subclasses. |

Toda linha de `SEGURANCA` precisa de `NOME` e `DESCRICAO`, inclusive a de usuário.

---

## UserEntity

**Classe**: `br.com.archbase.security.persistence.UserEntity`, `@DiscriminatorValue("USUARIO")`
**Tabela**: `SEGURANCA`
Implementa `org.springframework.security.core.userdetails.UserDetails`.

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `userName` | `USER_NAME` | `String` | sim | Retornado por `getUsername()`. |
| `password` | `SENHA` | `String` | sim | Hash da senha. |
| `changePasswordOnNextLogin` | `BO_ALTERAR_SENHA_PROXIMO_LOGIN` (1) | `Boolean` | sim | `S`/`N`. |
| `allowPasswordChange` | `BO_PERMITE_ALTERAR_SENHA` (1) | `Boolean` | sim | `S`/`N`. |
| `allowMultipleLogins` | `BO_PERMITE_MULTIPLICOS_LOGINS` (1) | `Boolean` | sim | `S`/`N`. Sem ela (`N`/nulo), login novo revoga todas as sessões anteriores. |
| `passwordNeverExpires` | `BO_SENHA_NUNCA_EXPIRA` (1) | `Boolean` | sim | `S`/`N`. |
| `passwordChangedAt` | `DT_ULTIMA_TROCA_SENHA` | `LocalDateTime` | sim | Base da expiração periódica. Nula em usuários anteriores à coluna. |
| `accountDeactivated` | `BO_CONTA_DESATIVADA` (1) | `Boolean` | sim | `S`/`N`. |
| `accountLocked` | `CONTA_BLOQUEADA` (1) | `Boolean` | sim | `S`/`N`. Sem prefixo `BO_`. |
| `unlimitedAccessHours` | `BO_HORARIO_LIVRE` (1) | `Boolean` | sim | `S`/`N`. **Gravado, nunca aplicado** — ver [horário de acesso](#horário-de-acesso-não-é-aplicado). |
| `isAdministrator` | `BO_ADMINISTRADOR` (1) | `Boolean` | sim | `S`/`N`. Nulo é tratado como não-administrador (com `WARN` no log). |
| `accessSchedule` | `HORARIO_ACESSO_ID` | `AccessScheduleEntity` | sim | `@ManyToOne`. **Não aplicado.** |
| `profile` | `PERFIL_ID` | `ProfileEntity` | sim | `@ManyToOne`. Um perfil por usuário. |
| `avatar` | `AVATAR` (16777215) | `byte[]` | sim | Sem `@Lob`: `bytea` no PostgreSQL, `MEDIUMBLOB` no MySQL. |
| `email` | `EMAIL` | `String` | sim | |
| `nickname` | `APELIDO` | `String` | sim | |
| `externalId` | `EXTERNAL_ID` | `String` | sim | `unique = true`. Id da conta num provedor de identidade. |
| `employeeId` | `EMPLOYEE_ID` (60) | `String` | sim | Matrícula. Opcional e **não única**, de propósito. |
| `mfaEnabled` | `BO_MFA_HABILITADO` (1) | `Boolean` | sim | `S`/`N`. |
| `mfaSecret` | `MFA_SECRET` (255) | `String` | sim | Segredo TOTP. |
| `mfaRecoveryCodes` | `MFA_RECOVERY_CODES` (2048) | `String` | sim | Códigos de recuperação. |
| `groups` | — | `Set<UserGroupEntity>` | — | `@OneToMany(mappedBy = "user", cascade = ALL, orphanRemoval = true)`. |
| `tokens` | — | `List<AccessTokenEntity>` | — | `@OneToMany(mappedBy = "user", cascade = ALL, orphanRemoval = true)`, `@NotAudited`. |

**Unicidade.** A única restrição de unicidade mapeada em `UserEntity` é `EXTERNAL_ID`. **Não há**
único `(TENANT_ID, USER_NAME)` nem `(TENANT_ID, EMAIL)` no mapeamento JPA; se a sua base precisa
deles, crie no banco.

**Não existem** em `UserEntity`: coleção de tokens de API, coleção de tokens de redefinição de
senha, `getPasswordExpired()`, colunas de "senha expirada" ou "data de expiração da senha". A
expiração é calculada (abaixo).

### UserDetails

| Método | Retorna |
|--------|---------|
| `getUsername()` | `userName` |
| `getAuthorities()` | **lista vazia, sempre.** Autorização é do core (`@HasPermission` etc.); `hasRole(...)`/`hasAuthority(...)` do Spring sempre negam. |
| `isAccountNonExpired()` | `accountDeactivated` não é `TRUE` |
| `isAccountNonLocked()` | `accountLocked` não é `TRUE` |
| `isEnabled()` | nem `accountDeactivated` nem `accountLocked` são `TRUE` |
| `isCredentialsNonExpired()` | `false` se `changePasswordOnNextLogin = TRUE`; senão, se `passwordNeverExpires = FALSE`, confere `DT_ULTIMA_TROCA_SENHA` (ou `DH_CRIACAO`, se nula) contra `archbase.security.password.expiration-days`; nos demais casos `true`. |

`markPasswordChanged()` zera `changePasswordOnNextLogin` e grava `DT_ULTIMA_TROCA_SENHA = agora`.

O administrador (`isAdministrator = TRUE`) só é tratado como tal se `isEnabled()` for verdadeiro, e
mesmo assim uma permissão `DENY` que o alcance vence (ver [permissões](permission-entities.md#permissionentity)).

---

## GroupEntity

**Classe**: `br.com.archbase.security.persistence.GroupEntity`, `@DiscriminatorValue("SEGURANCA_GRUPO")`
**Tabela**: `SEGURANCA`

Não declara colunas próprias: tem só `ID_SEGURANCA`, `CD_SEGURANCA`, `NOME`, `DESCRICAO` e as herdadas.
Os membros estão em `SEGURANCA_GRUPO_USUARIO`. Um usuário pode estar em vários grupos.

---

## ProfileEntity

**Classe**: `br.com.archbase.security.persistence.ProfileEntity`, `@DiscriminatorValue("SEGURANCA_PERFIL")`
**Tabela**: `SEGURANCA`

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `accessLevel` | `ACCESS_LEVEL` (30) | `AccessLevel` | sim | `AccessLevelConverter`. `NONE`, `READER`, `OPERATOR`, `SUPERVISOR`, `TENANT_ADMIN`. |

O nível mora no perfil porque o perfil é **um por usuário** e o piso precisa de valor único. Nulo
significa sem nível, e então vale `archbase.security.access-level.default`. O nível só pesa com
`archbase.security.access-level.enabled=true` (portão `LEVEL`). Quem modela senioridade fora do
perfil implementa `ArchbaseAccessLevelResolver`.

**Grupo × perfil.** Os dois recebem permissões pela mesma coluna (`SEGURANCA_PERMISSAO.ID_SEGURANCA`).
A diferença estrutural é a cardinalidade: perfil é um por usuário (`PERFIL_ID`), grupo é N por
usuário (`SEGURANCA_GRUPO_USUARIO`), e só o perfil carrega nível.

---

## UserGroupEntity

**Classe**: `br.com.archbase.security.persistence.UserGroupEntity`
**Tabela**: `SEGURANCA_GRUPO_USUARIO` (auditada)

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_USUARIO_GRUPO` (40) | `String` | não (PK) | |
| `code` | `CD_USUARIO_GRUPO` (40) | `String` | sim | |
| `user` | `ID_USUARIO` | `UserEntity` | sim | `@ManyToOne`. |
| `group` | `ID_GRUPO` | `GroupEntity` | sim | `@ManyToOne`. |

Não há restrição de unicidade mapeada para `(ID_USUARIO, ID_GRUPO)`.

---

## Horário de acesso: não é aplicado

`AccessScheduleEntity`, `AccessIntervalEntity`, `UserEntity.accessSchedule` e
`UserEntity.unlimitedAccessHours` existem, são gravados pelos cadastros e trafegam nos DTOs — mas
**o framework não aplica horário de acesso em lugar nenhum**: nem no login, nem no filtro JWT, nem
no core de autorização. Um usuário com `BO_HORARIO_LIVRE = 'N'` e um horário restrito entra e usa o
sistema a qualquer hora, administrador ou não. Se a aplicação precisa da restrição, ela tem de
implementá-la. As tabelas continuam no esquema por compatibilidade.

## AccessScheduleEntity

**Classe**: `br.com.archbase.security.persistence.AccessScheduleEntity`
**Tabela**: `SEGURANCA_HORARIO_ACESSO` (não auditada)

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_HORARIO_ACESSO` (40) | `String` | não (PK) | |
| `code` | `CD_HORARIO_ACESSO` (40) | `String` | sim | |
| `description` | `DESCRICAO` | `String` | sim | |
| `intervals` | — | `List<AccessIntervalEntity>` | — | `@OneToMany(cascade = ALL, orphanRemoval = true)` com `@JoinColumn(name = "ID_HORARIO_ACESSO")` — a FK fica em `SEGURANCA_INTERVALO_ACESSO`. |

## AccessIntervalEntity

**Classe**: `br.com.archbase.security.persistence.AccessIntervalEntity`
**Tabela**: `SEGURANCA_INTERVALO_ACESSO` (não auditada)

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_INTERVALO_ACESSO` (40) | `String` | não (PK) | |
| `code` | `CD_INTERVALO_ACESSO` (40) | `String` | sim | |
| `accessSchedule` | `ID_HORARIO_ACESSO` | `AccessScheduleEntity` | sim | `@ManyToOne`, mesma coluna da `@JoinColumn` do lado do horário. |
| `dayOfWeek` | `DIA_DA_SEMANA` | `Long` | sim | Número; o framework não define a convenção (não há quem leia). |
| `startTime` | `HORA_INICIAL` | `String` | sim | Texto livre; nenhum formato é validado. |
| `endTime` | `HORA_FINAL` | `String` | sim | Idem. |

---

## Resumo

| Classe | Tabela | Colunas próprias |
|--------|--------|------------------|
| `SecurityEntity` | `SEGURANCA` | `ID_SEGURANCA`, `CD_SEGURANCA`, `TP_SEGURANCA`, `NOME`, `DESCRICAO` |
| `UserEntity` | `SEGURANCA` | 21 colunas (tabela acima) |
| `GroupEntity` | `SEGURANCA` | nenhuma |
| `ProfileEntity` | `SEGURANCA` | `ACCESS_LEVEL` |
| `UserGroupEntity` | `SEGURANCA_GRUPO_USUARIO` | `ID_USUARIO_GRUPO`, `CD_USUARIO_GRUPO`, `ID_USUARIO`, `ID_GRUPO` |
| `AccessScheduleEntity` | `SEGURANCA_HORARIO_ACESSO` | `ID_HORARIO_ACESSO`, `CD_HORARIO_ACESSO`, `DESCRICAO` |
| `AccessIntervalEntity` | `SEGURANCA_INTERVALO_ACESSO` | `ID_INTERVALO_ACESSO`, `CD_INTERVALO_ACESSO`, `ID_HORARIO_ACESSO`, `DIA_DA_SEMANA`, `HORA_INICIAL`, `HORA_FINAL` |

---

[← Anterior: Visão Geral](overview.md) | [Voltar ao Índice](../README.md) | [Próximo: Sistema de Permissões →](permission-entities.md)
