-- Archbase 2.x -> 3.x — colunas que o archbase-security passou a mapear (PostgreSQL)
--
-- Aplique antes de subir a aplicação com o archbase 3.x. Sem isto, a primeira consulta a
-- seguranca_acao falha com "column ae1_0.minimum_level does not exist" e a aplicação não sobe.
--
-- Só é necessário para quem tem o schema sob controle de migrations (Flyway, Liquibase) ou com
-- hbm2ddl.auto=none/validate. Quem roda com hbm2ddl.auto=update recebe as colunas do Hibernate.
--
-- Nada aqui muda comportamento: todas as colunas são anuláveis, o código convive com nulo e as
-- proteções que dependem delas vêm desligadas por padrão (ver deployment/security-hardening.md).
--
-- É idempotente: pode rodar mais de uma vez.
--
-- SOBRE OS NOMES: estão em minúsculas porque é assim que a estratégia de nomenclatura padrão do
-- Spring Boot os grava. Se o seu projeto usa PhysicalNamingStrategyStandardImpl (que preserva o
-- nome da anotação, em maiúsculas) ou outra estratégia própria, confira antes com
--   SELECT column_name FROM information_schema.columns WHERE table_name = 'seguranca_acao';

-- ---------------------------------------------------------------------------------------------
-- seguranca — tabela única de usuário, grupo e perfil (herança SINGLE_TABLE)
-- ---------------------------------------------------------------------------------------------

-- Perfil: piso de capacidade do portão LEVEL do core de autorização.
-- Inerte enquanto archbase.security.access-level.enabled for false, que é o padrão.
ALTER TABLE seguranca ADD COLUMN IF NOT EXISTS access_level varchar(30);

-- Usuário: avatar e segundo fator de autenticação.
ALTER TABLE seguranca ADD COLUMN IF NOT EXISTS avatar bytea;
ALTER TABLE seguranca ADD COLUMN IF NOT EXISTS bo_mfa_habilitado varchar(1);
ALTER TABLE seguranca ADD COLUMN IF NOT EXISTS mfa_secret varchar(255);
ALTER TABLE seguranca ADD COLUMN IF NOT EXISTS mfa_recovery_codes varchar(2048);

-- ---------------------------------------------------------------------------------------------
-- Demais tabelas de segurança
-- ---------------------------------------------------------------------------------------------

-- Ação: nível mínimo exigido, gravado no primeiro registro da ação.
ALTER TABLE seguranca_acao ADD COLUMN IF NOT EXISTS minimum_level varchar(30);

-- Permissão: GRANT ou DENY. Nulo continua valendo como concessão — o comportamento de antes.
ALTER TABLE seguranca_permissao ADD COLUMN IF NOT EXISTS effect varchar(10);

-- Token de acesso: distingue access de refresh. As linhas existentes ficam nulas e seguem sendo
-- tratadas como antes, já que archbase.security.jwt.strict-token-use vem desligado.
ALTER TABLE seguranca_token_acesso ADD COLUMN IF NOT EXISTS tp_uso_token varchar(20);

-- Token de API: hash do token. O valor em claro continua na coluna token enquanto
-- archbase.security.api-token.purge-plaintext estiver desligado.
ALTER TABLE seguranca_token_api ADD COLUMN IF NOT EXISTS token_hash varchar(64);

-- A coluna token deixou de ser obrigatória na entidade. É o que permite, mais adiante, apagar o
-- valor em claro sem violar a restrição; sem isto, ligar o purge quebra a gravação.
ALTER TABLE seguranca_token_api ALTER COLUMN token DROP NOT NULL;

-- ---------------------------------------------------------------------------------------------
-- Adições posteriores à 3.0 (3.1.20 e 3.4). Necessárias para quem vai do 2.x direto para 3.4+.
-- ---------------------------------------------------------------------------------------------

-- Usuário: matrícula do funcionário (3.1.20). Opcional e sem índice único.
ALTER TABLE seguranca ADD COLUMN IF NOT EXISTS employee_id varchar(60);

-- Ação: rótulo curto da capacidade (3.4). Nulo = usa a descrição.
ALTER TABLE seguranca_acao ADD COLUMN IF NOT EXISTS rotulo varchar(120);

-- Dependências entre capacidades (3.4). Informativa: a decisão de acesso não lê esta tabela.
CREATE TABLE IF NOT EXISTS seguranca_acao_dependencia (
    id_dependencia         varchar(40)  not null,
    cd_dependencia         varchar(40),
    tenant_id              varchar(40),
    id_acao                varchar(40)  not null,
    capacidade_requerida   varchar(200) not null,
    id_acao_requerida      varchar(40),
    declarada_por          varchar(20)  not null,
    versao                 bigint,
    dh_criacao             timestamp(6),
    usuario_criou          varchar(255),
    dh_atualizacao         timestamp(6),
    ultimo_usuario_alterou varchar(255),
    constraint pk_seguranca_acao_dependencia primary key (id_dependencia),
    constraint fk_seguranca_acao_dependencia_acao
        foreign key (id_acao) references seguranca_acao (id_acao),
    constraint fk_seguranca_acao_dependencia_requerida
        foreign key (id_acao_requerida) references seguranca_acao (id_acao)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_seguranca_acao_dependencia
    ON seguranca_acao_dependencia (tenant_id, id_acao, capacidade_requerida);
CREATE INDEX IF NOT EXISTS idx_seguranca_acao_dependencia_acao
    ON seguranca_acao_dependencia (id_acao);

-- As tabelas seguranca_evento e seguranca_revisao (trilha e revisões) vêm de
-- auditoria-seguranca-postgresql.sql. Com ddl-auto=validate elas são exigidas mesmo com a trilha
-- desligada: rode aquele script também.
