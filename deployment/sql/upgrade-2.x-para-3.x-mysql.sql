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

-- Permissão: ALLOW ou DENY. Nulo continua valendo como concessão.
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
