-- Trilha de auditoria da segurança — tabelas (PostgreSQL)
--
-- Rode ANTES de ligar archbase.security.audit.enabled=true. A ordem importa: com a chave ligada e
-- as tabelas ausentes, a primeira alteração de permissão falha.
--
-- Só é necessário para quem controla o schema por migrations (hbm2ddl.auto=none ou validate). Com
-- hbm2ddl.auto=update o Hibernate cria tudo isto sozinho.
--
-- O que cada grupo guarda:
--   seguranca_revisao  — uma linha por alteração: quem, quando, de qual tenant e de onde;
--   *_aud              — o estado de cada entidade em cada revisão, incluindo o anterior;
--   seguranca_evento   — o que não altera tabela: login, falha, logout, negação, simulação.
--
-- ESTE ARQUIVO É GERADO a partir do mapeamento real das entidades, não escrito à mão. Se uma
-- entidade auditada ganhar campo, a coluna correspondente precisa entrar aqui — o Envers falha ao
-- gravar numa coluna que não existe.


create sequence seguranca_revisao_seq start with 1 increment by 50;

create table seguranca_acao_aud (bo_ativa varchar(1) check ((bo_ativa in ('N','S','N'))), tp_revisao smallint, id_revisao bigint not null, minimum_level varchar(30) check ((minimum_level in ('OPERATOR','SUPERVISOR','READER','NONE','TENANT_ADMIN'))), id_acao varchar(40) not null, id_recurso varchar(40), categoria varchar(255), descricao varchar(255), nome varchar(255), versao_acao varchar(255), primary key (id_revisao, id_acao));

create table seguranca_aud (bo_administrador varchar(1) check ((bo_administrador in ('N','S','N'))), bo_alterar_senha_proximo_login varchar(1) check ((bo_alterar_senha_proximo_login in ('N','S','N'))), bo_conta_desativada varchar(1) check ((bo_conta_desativada in ('N','S','N'))), bo_horario_livre varchar(1) check ((bo_horario_livre in ('N','S','N'))), bo_mfa_habilitado varchar(1) check ((bo_mfa_habilitado in ('N','S','N'))), bo_permite_alterar_senha varchar(1) check ((bo_permite_alterar_senha in ('N','S','N'))), bo_permite_multiplicos_logins varchar(1) check ((bo_permite_multiplicos_logins in ('N','S','N'))), bo_senha_nunca_expira varchar(1) check ((bo_senha_nunca_expira in ('N','S','N'))), conta_bloqueada varchar(1) check ((conta_bloqueada in ('N','S','N'))), tp_revisao smallint, dt_ultima_troca_senha timestamp(6), id_revisao bigint not null, access_level varchar(30) check ((access_level in ('OPERATOR','SUPERVISOR','READER','NONE','TENANT_ADMIN'))), tp_seguranca varchar(31) not null, horario_acesso_id varchar(40), id_seguranca varchar(40) not null, perfil_id varchar(40), mfa_recovery_codes varchar(2048), apelido varchar(255), descricao varchar(255), email varchar(255), external_id varchar(255), mfa_secret varchar(255), nome varchar(255), senha varchar(255), user_name varchar(255), avatar bytea, primary key (id_revisao, id_seguranca));

create table seguranca_evento (bo_sucesso boolean not null, dh_evento timestamp(6) not null, id_evento varchar(40) not null, tp_evento varchar(40) not null check ((tp_evento in ('LOGIN','LOGIN_FALHOU','LOGOUT','ACESSO_NEGADO','SIMULACAO'))), origem varchar(100), detalhe varchar(500), acao varchar(255), recurso varchar(255), tenant_id varchar(255), usuario varchar(255), primary key (id_evento));

create table seguranca_grupo_usuario_aud (tp_revisao smallint, id_revisao bigint not null, id_grupo varchar(40), id_usuario varchar(40), id_usuario_grupo varchar(40) not null, primary key (id_revisao, id_usuario_grupo));

create table seguranca_permissao_aud (tp_revisao smallint, id_revisao bigint not null, effect varchar(10) check ((effect in ('GRANT','DENY'))), id_acao varchar(40), id_permissao varchar(40) not null, id_seguranca varchar(40), company_id varchar(255), project_id varchar(255), primary key (id_revisao, id_permissao));

create table seguranca_recurso_aud (bo_ativo varchar(1) check ((bo_ativo in ('N','S','N'))), tp_revisao smallint, id_revisao bigint not null, id_recurso varchar(40) not null, tipo_recurso varchar(50) check ((tipo_recurso in ('VIEW','API'))), descricao varchar(255), nome varchar(255), primary key (id_revisao, id_recurso));

create table seguranca_revisao (dh_revisao timestamp(6), dh_revisao_milis bigint, id_revisao bigint not null, origem varchar(100), tenant_id varchar(255), usuario varchar(255), primary key (id_revisao));

create index IDX_SEGURANCA_EVENTO_DH on seguranca_evento (dh_evento);

create index IDX_SEGURANCA_EVENTO_TENANT on seguranca_evento (tenant_id);

create index IDX_SEGURANCA_EVENTO_USUARIO on seguranca_evento (usuario);

create index IDX_SEGURANCA_REVISAO_TENANT on seguranca_revisao (tenant_id);

create index IDX_SEGURANCA_REVISAO_DH on seguranca_revisao (dh_revisao);

alter table if exists seguranca_acao_aud add constraint FKidnj7wonuyr2u1b4q025meas0 foreign key (id_revisao) references seguranca_revisao;

alter table if exists seguranca_aud add constraint FK72uwfo7i9hwl1kcql217t8de1 foreign key (id_revisao) references seguranca_revisao;

alter table if exists seguranca_grupo_usuario_aud add constraint FKjcs19ed3ci24l66b9i6p0heay foreign key (id_revisao) references seguranca_revisao;

alter table if exists seguranca_permissao_aud add constraint FK2ttm05w3jcodr46gop2cyb22q foreign key (id_revisao) references seguranca_revisao;

alter table if exists seguranca_recurso_aud add constraint FKg3itdiklgvmqvbmp1newf3bb1 foreign key (id_revisao) references seguranca_revisao;
