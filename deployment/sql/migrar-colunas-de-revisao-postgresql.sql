-- Migra as colunas de revisão das tabelas _AUD para a nomenclatura do Archbase (PostgreSQL)
-- ============================================================================================
--
-- QUANDO USAR
-- Base cujas tabelas de auditoria foram criadas por uma versão do framework entre a 3.1.12 e a
-- 3.1.16 com a trilha DESLIGADA. Nesse caso elas nasceram com o padrão do Envers — "rev" e
-- "revtype" — e não com "id_revisao"/"tp_revisao", que é o que a aplicação usa quando a trilha é
-- ligada. Ligar a trilha depois derruba a aplicação no primeiro registro:
--
--     ERROR: null value in column "rev" of relation "seguranca_recurso_aud"
--     violates not-null constraint (SQLSTATE 23502)
--
-- A partir da 3.1.19 o framework faz o RENAME sozinho na subida. Este script existe para o caso
-- que ele NÃO resolve: quando as DUAS nomenclaturas já convivem na mesma tabela, porque uma versão
-- aditiva acrescentou "id_revisao" sem migrar o "rev". Aí uma das colunas guarda o histórico e a
-- escolha não pode ser automática.
--
-- É SEGURO RODAR EM QUALQUER ESTADO
-- Cada tabela é inspecionada antes de ser tocada. Tabela ausente, coluna ausente, coluna já no
-- nome certo, tabela já migrada — todos são tratados e simplesmente pulados. Rodar duas vezes tem
-- o mesmo efeito de rodar uma. O script não cria tabela nem apaga linha.
--
-- O QUE ELE FAZ, POR TABELA
--   1. só "rev"                      -> RENAME para id_revisao (preserva tudo)
--   2. "rev" E "id_revisao"          -> copia rev para id_revisao onde este estiver nulo,
--                                       recria a PK trocando rev por id_revisao, remove rev
--   3. só "id_revisao"               -> nada a fazer
--   idem para revtype -> tp_revisao
--   ao final, id_revisao vira NOT NULL (só se não houver nulo remanescente)
--
-- ANTES DE RODAR
-- Faça backup. O passo 2 remove uma coluna, e coluna removida não volta. Rode primeiro o bloco de
-- DIAGNÓSTICO abaixo para ver em que estado cada tabela está.
--
-- Tudo roda numa transação: se qualquer passo falhar, nada é aplicado.

-- ─────────────────────────── DIAGNÓSTICO (rode antes, sozinho) ───────────────────────────
-- Mostra o estado de cada tabela de auditoria sem alterar nada.
--
--   SELECT c.relname AS tabela,
--          bool_or(a.attname = 'rev')        AS tem_rev_legado,
--          bool_or(a.attname = 'id_revisao') AS tem_id_revisao,
--          bool_or(a.attname = 'revtype')    AS tem_revtype_legado,
--          bool_or(a.attname = 'tp_revisao') AS tem_tp_revisao
--     FROM pg_class c
--     JOIN pg_namespace n ON n.oid = c.relnamespace
--     JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
--    WHERE n.nspname = current_schema()
--      AND c.relkind = 'r'
--      AND c.relname LIKE 'seguranca%\_aud'
--    GROUP BY c.relname
--    ORDER BY c.relname;

-- ─────────────────────────────────── MIGRAÇÃO ───────────────────────────────────
BEGIN;

DO $migracao$
DECLARE
    tabela            text;
    tem_rev           boolean;
    tem_id_revisao    boolean;
    tem_revtype       boolean;
    tem_tp_revisao    boolean;
    nome_pk           text;
    colunas_pk        text;
    nulos             bigint;
    migradas          int := 0;
BEGIN
    FOR tabela IN
        SELECT c.relname
          FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
         WHERE n.nspname = current_schema()
           AND c.relkind = 'r'
           AND c.relname LIKE 'seguranca%\_aud'
         ORDER BY c.relname
    LOOP
        SELECT bool_or(attname = 'rev'), bool_or(attname = 'id_revisao'),
               bool_or(attname = 'revtype'), bool_or(attname = 'tp_revisao')
          INTO tem_rev, tem_id_revisao, tem_revtype, tem_tp_revisao
          FROM pg_attribute
         WHERE attrelid = (quote_ident(current_schema()) || '.' || quote_ident(tabela))::regclass
           AND attnum > 0 AND NOT attisdropped;

        -- ── Coluna de revisão ────────────────────────────────────────────────
        IF tem_rev AND NOT tem_id_revisao THEN
            -- Caso simples: só renomear. Nenhuma linha é tocada.
            EXECUTE format('ALTER TABLE %I RENAME COLUMN rev TO id_revisao', tabela);
            RAISE NOTICE '% : rev renomeada para id_revisao', tabela;
            migradas := migradas + 1;

        ELSIF tem_rev AND tem_id_revisao THEN
            -- As duas convivem. O histórico está em rev; id_revisao só tem valor nas linhas
            -- gravadas depois que a coluna foi acrescentada.
            EXECUTE format('UPDATE %I SET id_revisao = rev WHERE id_revisao IS NULL', tabela);

            -- rev costuma fazer parte da chave primária: sem trocar a PK, o DROP COLUMN falha.
            SELECT con.conname,
                   string_agg(quote_ident(att.attname), ', ' ORDER BY k.ord)
              INTO nome_pk, colunas_pk
              FROM pg_constraint con
              JOIN LATERAL unnest(con.conkey) WITH ORDINALITY AS k(attnum, ord) ON true
              JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = k.attnum
             WHERE con.conrelid = (quote_ident(current_schema()) || '.' || quote_ident(tabela))::regclass
               AND con.contype = 'p'
             GROUP BY con.conname;

            IF nome_pk IS NOT NULL AND colunas_pk LIKE '%rev%' THEN
                EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', tabela, nome_pk);
                EXECUTE format('ALTER TABLE %I ADD PRIMARY KEY (%s)',
                               tabela, replace(colunas_pk, 'rev', 'id_revisao'));
                RAISE NOTICE '% : chave primária recriada sobre id_revisao', tabela;
            END IF;

            EXECUTE format('ALTER TABLE %I DROP COLUMN rev', tabela);
            RAISE NOTICE '% : histórico copiado de rev para id_revisao e rev removida', tabela;
            migradas := migradas + 1;
        END IF;

        -- ── Coluna de tipo de revisão ────────────────────────────────────────
        IF tem_revtype AND NOT tem_tp_revisao THEN
            EXECUTE format('ALTER TABLE %I RENAME COLUMN revtype TO tp_revisao', tabela);
            RAISE NOTICE '% : revtype renomeada para tp_revisao', tabela;

        ELSIF tem_revtype AND tem_tp_revisao THEN
            EXECUTE format('UPDATE %I SET tp_revisao = revtype WHERE tp_revisao IS NULL', tabela);
            EXECUTE format('ALTER TABLE %I DROP COLUMN revtype', tabela);
            RAISE NOTICE '% : histórico copiado de revtype para tp_revisao e revtype removida', tabela;
        END IF;

        -- ── NOT NULL, só quando não sobrar nulo ──────────────────────────────
        -- Aplicar com linha nula falharia e derrubaria a transação inteira. Se sobrou nulo, algo
        -- fugiu do previsto e é melhor deixar a coluna como está do que perder a migração toda.
        EXECUTE format('SELECT count(*) FROM %I WHERE id_revisao IS NULL', tabela) INTO nulos;
        IF nulos = 0 THEN
            EXECUTE format('ALTER TABLE %I ALTER COLUMN id_revisao SET NOT NULL', tabela);
        ELSE
            RAISE WARNING '% : % linha(s) com id_revisao nulo — NOT NULL não aplicado. Verifique antes de ligar a trilha.',
                          tabela, nulos;
        END IF;
    END LOOP;

    IF migradas = 0 THEN
        RAISE NOTICE 'Nenhuma tabela precisou de migração — as colunas de revisão já estão no padrão do Archbase.';
    ELSE
        RAISE NOTICE '% tabela(s) migradas.', migradas;
    END IF;
END
$migracao$;

COMMIT;

-- ─────────────────────────── CONFERÊNCIA (rode depois) ───────────────────────────
-- Espera-se: tem_rev_legado e tem_revtype_legado FALSE em todas; as outras duas TRUE.
--
--   SELECT c.relname AS tabela,
--          bool_or(a.attname = 'rev')        AS tem_rev_legado,
--          bool_or(a.attname = 'id_revisao') AS tem_id_revisao,
--          bool_or(a.attname = 'revtype')    AS tem_revtype_legado,
--          bool_or(a.attname = 'tp_revisao') AS tem_tp_revisao
--     FROM pg_class c
--     JOIN pg_namespace n ON n.oid = c.relnamespace
--     JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
--    WHERE n.nspname = current_schema()
--      AND c.relkind = 'r'
--      AND c.relname LIKE 'seguranca%\_aud'
--    GROUP BY c.relname
--    ORDER BY c.relname;
