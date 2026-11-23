-- Archbase 2.x -> 3.x — colunas que o archbase-security passou a mapear (MySQL 8)
--
-- Mesma finalidade da versão PostgreSQL; ver o cabeçalho daquele arquivo para o contexto.
--
-- O MySQL 8 não tem ADD COLUMN IF NOT EXISTS. Para o script continuar podendo rodar mais de uma
-- vez, cada coluna é adicionada por instrução preparada, montada só quando a coluna ainda não
-- existe — 'DO 0' é o "não faça nada" quando já existe. Este formato é aceito pelo Flyway e pelo
-- cliente mysql, e não depende de DELIMITER nem de criar procedure.
--
-- NOMES DAS TABELAS: em minúsculas. Em Linux o MySQL diferencia maiúsculas de minúsculas nos
-- nomes de tabela; confira os seus antes de rodar, com
--   SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE();

-- ---------------------------------------------------------------------------------------------
-- seguranca — tabela única de usuário, grupo e perfil (herança SINGLE_TABLE)
-- ---------------------------------------------------------------------------------------------

-- Perfil: piso de capacidade do portão LEVEL. Inerte enquanto access-level.enabled for false.
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca ADD COLUMN access_level varchar(30) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca' AND column_name = 'access_level');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Usuário: avatar (mediumblob espelha o length 16777215 do mapeamento).
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca ADD COLUMN avatar mediumblob NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca' AND column_name = 'avatar');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Usuário: segundo fator. A flag é gravada como 'S'/'N' pelo BooleanToSNConverter, não como
-- booleano — por isso varchar(1).
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca ADD COLUMN bo_mfa_habilitado varchar(1) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca' AND column_name = 'bo_mfa_habilitado');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca ADD COLUMN mfa_secret varchar(255) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca' AND column_name = 'mfa_secret');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca ADD COLUMN mfa_recovery_codes varchar(2048) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca' AND column_name = 'mfa_recovery_codes');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------------------------------------------------------------------------------------------
-- Demais tabelas de segurança
-- ---------------------------------------------------------------------------------------------

-- Ação: nível mínimo exigido.
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca_acao ADD COLUMN minimum_level varchar(30) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_acao' AND column_name = 'minimum_level');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Permissão: GRANT ou DENY. Nulo continua valendo como concessão.
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca_permissao ADD COLUMN effect varchar(10) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_permissao' AND column_name = 'effect');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Token de acesso: distingue access de refresh.
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca_token_acesso ADD COLUMN tp_uso_token varchar(20) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_token_acesso' AND column_name = 'tp_uso_token');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Token de API: hash do token.
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca_token_api ADD COLUMN token_hash varchar(64) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_token_api' AND column_name = 'token_hash');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- A coluna token deixou de ser obrigatória na entidade. É o que permite, mais adiante, apagar o
-- valor em claro sem violar a restrição. MODIFY é idempotente.
ALTER TABLE seguranca_token_api MODIFY token varchar(255) NULL;

-- ---------------------------------------------------------------------------------------------
-- Adições posteriores à 3.0 (3.1.20 e 3.4). Necessárias para quem vai do 2.x direto para 3.4+.
-- ---------------------------------------------------------------------------------------------

-- Usuário: matrícula do funcionário (3.1.20). Opcional e sem índice único.
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca ADD COLUMN employee_id varchar(60) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca' AND column_name = 'employee_id');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Ação: rótulo curto da capacidade (3.4). Nulo = usa a descrição.
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca_acao ADD COLUMN rotulo varchar(120) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_acao' AND column_name = 'rotulo');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

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
    dh_criacao             datetime(6),
    usuario_criou          varchar(255),
    dh_atualizacao         datetime(6),
    ultimo_usuario_alterou varchar(255),
    constraint pk_seguranca_acao_dependencia primary key (id_dependencia),
    constraint uk_seguranca_acao_dependencia unique (tenant_id, id_acao, capacidade_requerida),
    constraint fk_seguranca_acao_dependencia_acao
        foreign key (id_acao) references seguranca_acao (id_acao),
    constraint fk_seguranca_acao_dependencia_requerida
        foreign key (id_acao_requerida) references seguranca_acao (id_acao)
);
-- O índice de id_acao é criado pelo MySQL junto com a FK.

-- As tabelas seguranca_evento e seguranca_revisao (trilha e revisões) vêm de
-- auditoria-seguranca-mysql.sql. Com ddl-auto=validate elas são exigidas mesmo com a trilha
-- desligada: rode aquele script também.
