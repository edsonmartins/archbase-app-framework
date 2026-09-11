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
-- ⚠ ESTE ARQUIVO É ESPECÍFICO DE POSTGRESQL
-- Usa `add column if not exists`, `alter column ... drop not null` e `comment on column` — sintaxe
-- que MySQL e Oracle não aceitam. O archbase-starter-flyway acrescenta este diretório às locations
-- de TODO projeto por padrão (ArchbaseFlywayProperties.includeArchbaseLocations = true), então numa
-- aplicação que não seja PostgreSQL o Flyway falha e a aplicação NÃO SOBE.
--
-- Nesse caso, desligue a inclusão e aplique o DDL equivalente pelo seu próprio versionamento:
--     archbase.flyway.include-archbase-locations=false
--
-- O mesmo vale para quem tem Flyway habilitado mas cria as tabelas `seguranca*` por
-- `spring.jpa.hibernate.ddl-auto`: o Flyway roda antes do Hibernate e não encontrará as tabelas.

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

-- ── 3.0.11: token de API deixa de ser guardado em claro (ApiTokenEntity) ───────────────────────
-- O token de API é a credencial em si. Guardado em claro, um dump da tabela (backup, réplica de
-- homologação, SELECT de suporte) entrega acesso direto a toda integração. Passa a valer o SHA-256,
-- e a autenticação busca por ele.
--
-- O valor em claro NÃO é apagado aqui: o hash é preenchido na subida da aplicação
-- (ArchbaseApiTokenHashMigrator) e a coluna antiga continua servindo de compatibilidade até que
-- archbase.security.api-token.purge-plaintext=true seja ligado deliberadamente — apagar é
-- irreversível e não deve acontecer como efeito colateral de uma migration.
alter table seguranca_token_api
    add column if not exists token_hash varchar(64);

-- TOKEN passa a ser nulável: nas linhas criadas a partir daqui ele nasce vazio.
alter table seguranca_token_api
    alter column token drop not null;

comment on column seguranca_token_api.token_hash is
    'SHA-256 (hex) do token de API. É por aqui que a autenticação busca.';
comment on column seguranca_token_api.token is
    'Valor em claro, apenas em linhas anteriores a 3.0.11. Removível com archbase.security.api-token.purge-plaintext=true.';

-- Consulta quente: toda requisição autenticada por token de API bate neste índice.
create index if not exists idx_seguranca_token_api_hash
    on seguranca_token_api (token_hash);

-- ---------------------------------------------------------------------------------------------
-- CORE ÚNICO DE AUTORIZAÇÃO — nível mínimo e negação explícita
--
-- As três colunas abaixo entram NULAS, e o comportamento só muda quando alguém as preenche ou liga
-- a flag correspondente. Um sistema existente sobe idêntico.
-- ---------------------------------------------------------------------------------------------

-- O piso que a capacidade exige. Nulo = sem piso, que é como toda ação existente fica.
-- O valor é semeado pelo código em @HasPermission(minimumLevel = ...) no primeiro registro da ação;
-- a partir daí quem manda é o admin, igual já acontece com a descrição.
alter table seguranca_acao
    add column if not exists minimum_level varchar(30);

-- O nível que o perfil confere. Mora no perfil, e não no grupo, porque o perfil é um por usuário e
-- um piso ordinal precisa de valor único. SEGURANCA é a tabela única da hierarquia de segurança
-- (usuário, grupo e perfil), então a coluna só faz sentido nas linhas de perfil.
alter table seguranca
    add column if not exists access_level varchar(30);

-- Se a linha de permissão soma ou subtrai. Nulo = GRANT, que é o que toda concessão existente
-- significa. DENY vence qualquer concessão de qualquer origem DENTRO DO MESMO ESCOPO — é o que
-- permite tirar uma pessoa de algo que o time inteiro tem, sem criar um grupo paralelo só para isso.
alter table seguranca_permissao
    add column if not exists effect varchar(10);

comment on column seguranca_acao.minimum_level is
    'Nivel minimo exigido pela capacidade: READER < OPERATOR < SUPERVISOR < TENANT_ADMIN. Nulo = sem piso. So e avaliado com archbase.security.access-level.enabled=true.';
comment on column seguranca.access_level is
    'Nivel conferido pelo perfil. Aplica-se apenas a linhas de perfil. Nulo cai em archbase.security.access-level.default.';
comment on column seguranca_permissao.effect is
    'GRANT (padrao, inclusive quando nulo) ou DENY. A negacao vence a concessao dentro do escopo em que foi declarada.';

-- Consulta quente: a negação é procurada em toda decisão que encontra concessão.
create index if not exists idx_seguranca_permissao_effect
    on seguranca_permissao (effect);

-- ── 3.1.20: matrícula do funcionário (UserEntity) ─────────────────────────────────────────────
-- Opcional e sem índice único: contas de serviço e usuários que não são funcionários não têm
-- matrícula, e bases herdadas de sistemas antigos costumam ter repetição. Quem quiser exigir
-- unicidade cria o índice decidindo o escopo — global ou por tenant —, que é o que o framework não
-- tem como adivinhar.
alter table seguranca
    add column if not exists employee_id varchar(60);

comment on column seguranca.employee_id is
    'Matricula do funcionario na empresa. Elo com RH, ponto e folha. Diferente de external_id, que identifica a conta no provedor de identidade.';

-- ── 3.4: dependências entre capacidades (ActionDependencyEntity) ──────────────────────────────
-- Uma aresta dirigida "a origem não serve para nada sem o alvo". Cobre tanto a dependência entre
-- dois endpoints quanto o botão de uma tela que chama um endpoint — são a mesma aresta.
-- Ver archbase-security/CONTRATO_DEPENDENCIAS_DE_CAPACIDADE.md.
--
-- INFORMATIVA, NUNCA PORTÃO. A decisão de acesso não lê esta tabela, e não há flag que a faça ler.
-- Base existente sobe idêntica: sem @HasPermission(requires=...) e sem 'requires' no payload das
-- telas, a tabela nasce e permanece vazia.
--
-- POR QUE O ALVO É TEXTO. A varredura não tem ordem garantida entre recursos — quando a aresta é
-- gravada, a ação alvo pode ainda não existir — e o alvo pode nunca existir (erro de digitação,
-- módulo não implantado). A aresta precisa ficar visível como não resolvida em vez de sumir.
-- id_acao_requerida é um espelho, reapontado a cada sincronização.
--
-- Este CREATE existe para quem roda ddl-auto=validate: o inicializador de schema do framework é um
-- SmartInitializingSingleton e roda DEPOIS da validação do Hibernate, então não chega a tempo.
-- O Flyway roda antes.
create table if not exists seguranca_acao_dependencia (
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

-- Uma aresta por par (origem, alvo). O tenant entra na chave como nas demais entidades do módulo.
create unique index if not exists uk_seguranca_acao_dependencia
    on seguranca_acao_dependencia (tenant_id, id_acao, capacidade_requerida);

-- Consulta quente: a tela pede as dependências de um conjunto de capacidades a cada abertura.
create index if not exists idx_seguranca_acao_dependencia_acao
    on seguranca_acao_dependencia (id_acao);

comment on table seguranca_acao_dependencia is
    'Arestas "a capacidade X precisa da capacidade Y". Informativas: a decisao de acesso nao as le.';
comment on column seguranca_acao_dependencia.capacidade_requerida is
    'O alvo em texto, no formato recurso:acao. E a chave — id_acao_requerida e apenas o espelho resolvido.';
comment on column seguranca_acao_dependencia.id_acao_requerida is
    'O alvo resolvido, ou nulo enquanto a capacidade alvo nao existir no catalogo.';
comment on column seguranca_acao_dependencia.declarada_por is
    'SCAN (@HasPermission, reconciliacao integral) ou REGISTER (tela, poda escopada a acao).';

-- ── 3.4: rótulo da capacidade, separado da descrição (ActionEntity) ───────────────────────────
-- DESCRICAO vinha fazendo três trabalhos: identificar a linha na tela de permissões, explicar o que
-- a ação faz e — via '->' embutido, que o cliente quebra na exibição — agrupar. O resultado é um
-- catálogo em que centenas de linhas se chamam "Criar X", "Editar X", "Listar X", geradas em massa
-- pelo frontend, e quem administra não consegue distinguir uma da outra.
--
-- A coluna entra NULA e continua nula em toda capacidade existente. Nulo significa "use a
-- descrição", então a tela mostra exatamente o que mostra hoje. NÃO há reescrita em massa: as
-- descrições atuais funcionam como rótulo, e trocá-las por conta própria substituiria um texto que
-- alguém conhece por outro que ninguém pediu.
--
-- CATEGORIA já existia e nunca foi preenchida por nenhum coletor — passa a ser, quando o código ou
-- a tela a declararem.
--
-- Para reescrever os textos de um catálogo já existente a partir do código, existe
-- archbase.security.sync.mode=refresh: usado UMA vez, de propósito, e depois desligado. Ele
-- descarta ajuste feito pelo admin nesses três campos, e por isso não é o padrão.
-- CATEGORIA entra junto, e não por simetria: ela é mapeada em ActionEntity desde sempre e nunca
-- esteve nesta migration. Passou despercebido enquanto nenhum coletor a preenchia e nenhuma tela a
-- lia — agora as duas coisas mudaram. Em projeto com ddl-auto=validate ou none, onde este arquivo é
-- a única fonte do schema, a coluna simplesmente não existia.
alter table seguranca_acao
    add column if not exists rotulo varchar(120),
    add column if not exists categoria varchar(255);

comment on column seguranca_acao.rotulo is
    'Rotulo curto da capacidade ("Aprovar custo"). Nulo = use a descricao. Distinto de DESCRICAO, que explica o que a acao faz, e de CATEGORIA, que agrupa.';
comment on column seguranca_acao.categoria is
    'Agrupamento das capacidades dentro do recurso ("Custos"). Substitui o "->" que era embutido na descricao.';
