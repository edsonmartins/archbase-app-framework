# archbase-starter-flyway

Convenções Flyway do Archbase: baseline automático para adotar Flyway sobre uma base que já existe, e a location
onde os módulos do framework publicam a evolução dos próprios esquemas.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-flyway</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

`flyway-core` e `spring-boot-flyway` (a auto-configuração Flyway do Boot 4). Não traz módulo de banco do Flyway:
adicione o do seu banco (por exemplo `flyway-database-postgresql` ou `flyway-mysql`) e tenha um `DataSource`.

## O que é preciso ligar

Nada além do Flyway do próprio Spring Boot, que já vem ligado (`spring.flyway.enabled`, padrão `true`). Com o
starter no classpath, as convenções valem sem propriedade nenhuma; `archbase.flyway.enabled=false` as desliga.

## Auto-configuração

Registrada: `ArchbaseFlywayAutoConfiguration`, antes de `FlywayAutoConfiguration`.

Condições: `Flyway` e `FlywayConfigurationCustomizer` no classpath, e `archbase.flyway.enabled` (padrão `true`).

Registra um único bean, `archbaseFlywayConventionsCustomizer` (`FlywayConfigurationCustomizer`), aplicado pela
`FlywayAutoConfiguration` do Boot. Ele:

1. define `baselineOnMigrate` com `archbase.flyway.baseline-on-migrate`;
2. define `baselineVersion` com `archbase.flyway.baseline-version`, se não estiver em branco;
3. se `archbase.flyway.include-archbase-locations=true`, **acrescenta** `classpath:db/migration/archbase` às
   locations já configuradas (não substitui; não duplica se já estiver lá).

## Propriedades

| Propriedade | Padrão | Efeito |
|---|---|---|
| `archbase.flyway.enabled` | `true` | `false` não registra o customizer; aí vale só `spring.flyway.*` |
| `archbase.flyway.baseline-on-migrate` | `true` | Baseline automático quando o esquema já tem tabelas |
| `archbase.flyway.baseline-version` | `0` | Versão do baseline; com `0`, a primeira migração `V1__` ainda roda sobre a base existente |
| `archbase.flyway.include-archbase-locations` | `true` | Acrescenta `classpath:db/migration/archbase` |

**Estas propriedades vencem as equivalentes do Spring Boot.** O customizer roda depois que o Boot aplica
`spring.flyway.*`, então `spring.flyway.baseline-on-migrate` e `spring.flyway.baseline-version` são sobrescritas
enquanto `archbase.flyway.enabled=true`. Para controlar essas duas pelo `spring.flyway.*`, desligue as convenções.
(O javadoc de `ArchbaseFlywayProperties` diz o contrário.)

## As migrações do framework

Hoje só o `archbase-security` publica em `db/migration/archbase`: `R__archbase_security_schema.sql`, migração
repetível e escrita para **PostgreSQL**. Migrações `R__` não entram na numeração `V` do projeto. Se o
`archbase-security` não estiver no classpath, a location fica vazia e o Flyway apenas avisa (com
`spring.flyway.fail-on-missing-locations` no padrão `false`). Em outro banco, desligue
`include-archbase-locations` e trate o esquema de segurança como descrito em
[esquema-de-seguranca.md](../deployment/esquema-de-seguranca.md).

O `archbase-offline-sync` guarda sua migração em `db/archbase-offline-sync/`, fora desta location: ela não é
aplicada por este starter.

## Referência

- Esquema de segurança e a relação com o Flyway: [deployment/esquema-de-seguranca.md](../deployment/esquema-de-seguranca.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
