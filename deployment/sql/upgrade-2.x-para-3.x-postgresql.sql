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

-- Permissão: ALLOW ou DENY. Nulo continua valendo como concessão — o comportamento de antes.
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
