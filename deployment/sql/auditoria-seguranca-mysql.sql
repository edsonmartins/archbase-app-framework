-- Trilha de auditoria da segurança — tabelas (MySQL 8)
--
-- Rode ANTES de ligar archbase.security.audit.enabled=true: com a chave ligada e as tabelas
-- ausentes, a primeira alteração de permissão falha.
--
-- Só é necessário para quem controla o schema por migrations. Com hbm2ddl.auto=update o Hibernate
-- cria tudo isto sozinho.
--
-- NOMES DE TABELA: em Linux o MySQL diferencia maiúsculas de minúsculas. Confira os seus.
--
-- ESTE ARQUIVO É GERADO a partir do mapeamento real das entidades, não escrito à mão. Índices e
-- chaves estrangeiras foram envolvidos em checagem no information_schema depois da geração: o MySQL 8
-- não aceita "create index if not exists", e "add constraint" repetido falha. Assim o script é
-- idempotente, como o de PostgreSQL.


create table if not exists seguranca_acao_aud (bo_ativa varchar(1), tp_revisao tinyint, id_revisao bigint not null, minimum_level varchar(30), id_acao varchar(40) not null, id_recurso varchar(40), categoria varchar(255), descricao varchar(255), nome varchar(255), rotulo varchar(120), versao_acao varchar(255), primary key (id_revisao, id_acao)) engine=InnoDB;

create table if not exists seguranca_aud (bo_administrador varchar(1), bo_alterar_senha_proximo_login varchar(1), bo_conta_desativada varchar(1), bo_horario_livre varchar(1), bo_mfa_habilitado varchar(1), bo_permite_alterar_senha varchar(1), bo_permite_multiplicos_logins varchar(1), bo_senha_nunca_expira varchar(1), conta_bloqueada varchar(1), tp_revisao tinyint, dt_ultima_troca_senha datetime(6), id_revisao bigint not null, access_level varchar(30), tp_seguranca varchar(31) not null, horario_acesso_id varchar(40), id_seguranca varchar(40) not null, perfil_id varchar(40), mfa_recovery_codes varchar(2048), apelido varchar(255), descricao varchar(255), email varchar(255), employee_id varchar(60), external_id varchar(255), mfa_secret varchar(255), nome varchar(255), senha varchar(255), user_name varchar(255), avatar mediumblob, primary key (id_revisao, id_seguranca)) engine=InnoDB;

create table if not exists seguranca_evento (bo_sucesso bit not null, dh_evento datetime(6) not null, id_evento varchar(40) not null, origem varchar(100), detalhe varchar(500), acao varchar(255), recurso varchar(255), tenant_id varchar(255), usuario varchar(255), tp_evento enum ('ACESSO_NEGADO','LOGIN','LOGIN_FALHOU','LOGOUT','SIMULACAO') not null, primary key (id_evento)) engine=InnoDB;

create table if not exists seguranca_grupo_usuario_aud (tp_revisao tinyint, id_revisao bigint not null, id_grupo varchar(40), id_usuario varchar(40), id_usuario_grupo varchar(40) not null, primary key (id_revisao, id_usuario_grupo)) engine=InnoDB;

create table if not exists seguranca_permissao_aud (tp_revisao tinyint, id_revisao bigint not null, effect varchar(10), id_acao varchar(40), id_permissao varchar(40) not null, id_seguranca varchar(40), company_id varchar(255), project_id varchar(255), primary key (id_revisao, id_permissao)) engine=InnoDB;

create table if not exists seguranca_recurso_aud (bo_ativo varchar(1), tp_revisao tinyint, id_revisao bigint not null, id_recurso varchar(40) not null, descricao varchar(255), nome varchar(255), tipo_recurso enum ('API','VIEW'), primary key (id_revisao, id_recurso)) engine=InnoDB;

create table if not exists seguranca_revisao (dh_revisao datetime(6), dh_revisao_milis bigint, id_revisao bigint not null, origem varchar(100), tenant_id varchar(255), usuario varchar(255), primary key (id_revisao)) engine=InnoDB;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'create index IDX_SEGURANCA_EVENTO_DH on seguranca_evento (dh_evento)', 'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_evento' AND index_name = 'IDX_SEGURANCA_EVENTO_DH');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'create index IDX_SEGURANCA_EVENTO_TENANT on seguranca_evento (tenant_id)', 'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_evento' AND index_name = 'IDX_SEGURANCA_EVENTO_TENANT');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'create index IDX_SEGURANCA_EVENTO_USUARIO on seguranca_evento (usuario)', 'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_evento' AND index_name = 'IDX_SEGURANCA_EVENTO_USUARIO');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'create index IDX_SEGURANCA_REVISAO_TENANT on seguranca_revisao (tenant_id)', 'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_revisao' AND index_name = 'IDX_SEGURANCA_REVISAO_TENANT');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'create index IDX_SEGURANCA_REVISAO_DH on seguranca_revisao (dh_revisao)', 'DO 0')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_revisao' AND index_name = 'IDX_SEGURANCA_REVISAO_DH');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'alter table seguranca_acao_aud add constraint FKidnj7wonuyr2u1b4q025meas0 foreign key (id_revisao) references seguranca_revisao (id_revisao)', 'DO 0')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_acao_aud' AND constraint_name = 'FKidnj7wonuyr2u1b4q025meas0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'alter table seguranca_aud add constraint FK72uwfo7i9hwl1kcql217t8de1 foreign key (id_revisao) references seguranca_revisao (id_revisao)', 'DO 0')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_aud' AND constraint_name = 'FK72uwfo7i9hwl1kcql217t8de1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'alter table seguranca_grupo_usuario_aud add constraint FKjcs19ed3ci24l66b9i6p0heay foreign key (id_revisao) references seguranca_revisao (id_revisao)', 'DO 0')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_grupo_usuario_aud' AND constraint_name = 'FKjcs19ed3ci24l66b9i6p0heay');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'alter table seguranca_permissao_aud add constraint FK2ttm05w3jcodr46gop2cyb22q foreign key (id_revisao) references seguranca_revisao (id_revisao)', 'DO 0')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_permissao_aud' AND constraint_name = 'FK2ttm05w3jcodr46gop2cyb22q');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'alter table seguranca_recurso_aud add constraint FKg3itdiklgvmqvbmp1newf3bb1 foreign key (id_revisao) references seguranca_revisao (id_revisao)', 'DO 0')
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_recurso_aud' AND constraint_name = 'FKg3itdiklgvmqvbmp1newf3bb1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Colunas auditadas que entraram depois da primeira versão deste script (3.1.20 e 3.4). O CREATE acima
-- já as traz; estes ALTER cobrem as bases em que as tabelas _aud foram criadas antes.
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca_aud ADD COLUMN employee_id varchar(60) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_aud' AND column_name = 'employee_id');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE seguranca_acao_aud ADD COLUMN rotulo varchar(120) NULL', 'DO 0')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'seguranca_acao_aud' AND column_name = 'rotulo');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
