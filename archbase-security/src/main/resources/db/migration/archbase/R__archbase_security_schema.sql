-- Schema de segurança do Archbase — evolução entregue pelo próprio framework.
--
-- POR QUE EXISTE
-- Ao subir de uma versão do Archbase para outra, as entidades de segurança podem ganhar colunas.
-- Sem este arquivo cada projeto descobre isso em runtime, uma coluna por vez: a aplicação nem sobe
-- ("Schema validation: missing column ... in table [seguranca]"), o time adivinha o tipo, escreve a
-- migration, tenta de novo, e descobre a próxima. Entregar o SQL junto do código que o exige é
-- responsabilidade do framework.
--
-- POR QUE REPEATABLE (R__) E NÃO VERSIONADA
-- Cada projeto tem sua própria numeração (V1, V2, ... V32). Uma migration versionada do framework
-- entraria fora de ordem no histórico do projeto e o Flyway a rejeitaria. Repeatable roda depois das
-- versionadas, sempre que o checksum muda, e não participa da ordenação.
--
-- POR QUE TODO COMANDO É IDEMPOTENTE
-- Este arquivo roda tanto em base nova quanto em base que já passou por upgrades manuais. Só
-- `IF NOT EXISTS` — nada aqui pode falhar por já ter sido aplicado, nem destruir dado existente.

-- ── 3.0: MFA/TOTP e rastreio de troca de senha (UserEntity) ────────────────────────────────────
alter table seguranca
    add column if not exists bo_mfa_habilitado varchar(1),
    add column if not exists mfa_secret varchar(255),
    add column if not exists mfa_recovery_codes varchar(2048),
    add column if not exists dt_ultima_troca_senha timestamp(6);

comment on column seguranca.bo_mfa_habilitado is 'Segundo fator TOTP habilitado (S/N)';
comment on column seguranca.mfa_secret is 'Segredo TOTP (Base32), cifrado em repouso';
comment on column seguranca.mfa_recovery_codes is 'Códigos de recuperação (hash bcrypt, um por linha)';
comment on column seguranca.dt_ultima_troca_senha is 'Quando a senha foi trocada pela última vez';

-- ── 3.0.11: refresh token passa a ser persistido e revogável (AccessTokenEntity) ───────────────
-- Antes, o refresh existia apenas como JWT assinado: logout, troca de senha e desativação de conta
-- só mexiam nas linhas de access token, e um refresh vazado seguia emitindo credenciais novas até
-- expirar sozinho. Agora as duas espécies dividem a tabela e esta coluna as separa.
alter table seguranca_token_acesso
    add column if not exists tp_uso_token varchar(20);

comment on column seguranca_token_acesso.tp_uso_token is
    'ACCESS ou REFRESH. NULL em linhas anteriores a 3.0.11, quando só access token era gravado — lidas como ACCESS.';

-- Sem índice novo de propósito: a busca do filtro é por TOKEN, que já é UNIQUE (e um índice em
-- varchar(5000) esbarraria no limite de entrada de btree do Postgres).
