# Atualizar do Archbase 2.x para o 3.x — mudanças de banco

O `archbase-security` 3.x mapeia nove colunas que a linha 2.x não tinha. Quem controla o schema por
migrations não as recebe sozinho, e a aplicação **não sobe**: a primeira consulta a `seguranca_acao`
falha com

```
ERROR: column ae1_0.minimum_level does not exist
```

Os scripts prontos estão em [`sql/`](sql/):

| Banco | Script |
|---|---|
| PostgreSQL | [`upgrade-2.x-para-3.x-postgresql.sql`](sql/upgrade-2.x-para-3.x-postgresql.sql) |
| MySQL 8 | [`upgrade-2.x-para-3.x-mysql.sql`](sql/upgrade-2.x-para-3.x-mysql.sql) |

Os dois são idempotentes — rodar de novo não faz nada.

## Preciso rodar isto?

| Sua configuração | Precisa? |
|---|---|
| `hbm2ddl.auto=none` ou `validate`, com Flyway ou Liquibase | **Sim** |
| `hbm2ddl.auto=update` | Não — o Hibernate cria as colunas |

## O que muda

Nada de comportamento. Todas as colunas são anuláveis, o código convive com nulo, e as proteções
que dependem delas vêm desligadas por padrão — ver [security-hardening.md](security-hardening.md).
A migração é só o schema alcançando o mapeamento.

| Tabela | Coluna | Para que serve |
|---|---|---|
| `seguranca` | `access_level` | Nível de acesso do perfil, portão LEVEL do core de autorização |
| `seguranca` | `avatar` | Imagem do usuário |
| `seguranca` | `bo_mfa_habilitado` | Segundo fator ligado (gravado `'S'`/`'N'`) |
| `seguranca` | `mfa_secret` | Segredo TOTP |
| `seguranca` | `mfa_recovery_codes` | Códigos de recuperação |
| `seguranca_acao` | `minimum_level` | Nível mínimo exigido pela ação |
| `seguranca_permissao` | `effect` | `ALLOW` ou `DENY`; nulo segue valendo como concessão |
| `seguranca_token_acesso` | `tp_uso_token` | Separa token de acesso de token de renovação |
| `seguranca_token_api` | `token_hash` | Hash do token de API |

Além das colunas, `seguranca_token_api.token` deixa de ser obrigatória. Sem isso, ligar
`archbase.security.api-token.purge-plaintext` mais tarde violaria a restrição na hora de apagar o
valor em claro.

`seguranca` é uma tabela só para usuário, grupo e perfil — o mapeamento usa herança
`SINGLE_TABLE`. Por isso colunas de perfil e de usuário convivem nela.

## Confira os nomes antes de rodar

Os scripts usam nomes em **minúsculas**, que é como a estratégia de nomenclatura padrão do Spring
Boot os grava. Se o seu projeto configura `PhysicalNamingStrategyStandardImpl` (preserva o nome da
anotação, em maiúsculas) ou uma estratégia própria, confira primeiro:

```sql
SELECT column_name FROM information_schema.columns WHERE table_name = 'seguranca_acao';
```

No MySQL sobre Linux os nomes de **tabela** também diferenciam maiúsculas de minúsculas.

## Como aplicar

Copie o conteúdo para uma migration nova do seu projeto, com a numeração que vier a seguir:

```
src/main/resources/db/migration/V36__archbase_security_3x_colunas_novas.sql
```

Ou rode direto, uma vez, na janela de manutenção do deploy.

## Outras quebras conhecidas na atualização

O 3.x acompanha Spring Boot 4, e a maior parte do trabalho de migração não é de banco. Os pontos que
apareceram em todos os projetos migrados até agora:

- `spring-boot-starter-aop` deixou de existir; use `spring-boot-starter-aspectj`
- o Boot 4 adota Jackson 3 (`tools.jackson`) e para de gerenciar a linha 2; se o projeto usa
  Jackson 2, importe o `jackson-bom` e declare `spring-boot-jackson2`
- o conversor HTTP passa a ser o do Jackson 3: um `ObjectMapper` do Jackson 2 configurado com
  formatos próprios **deixa de valer nas respostas REST**, sem erro nenhum
- `RestTemplateBuilder`, `EntityManagerFactoryBuilder`, `EntityScan` e `@WebMvcTest` mudaram de
  pacote; `@MockBean` virou `@MockitoBean`
- o `WebClient` saiu do `starter-webflux` e ganhou `spring-boot-starter-webclient`
- no Hibernate 7 a estratégia de nomenclatura padrão **deixou de converter** camelCase em
  snake_case quando o identificador está citado; quem liga `hibernate.globally_quoted_identifiers`
  perde a conversão em todas as entidades de uma vez
