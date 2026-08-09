# O esquema de segurança passa a vir com o framework

A partir da 3.1.12, o `archbase-security` confere o próprio esquema na subida da aplicação e cria o
que estiver faltando. Não é preciso configurar nada para isso acontecer.

## O que isso resolve

O código que exige uma tabela e o DDL que a cria sempre moraram no mesmo repositório — mas só o
primeiro era entregue. Cada atualização do framework virava uma caça: a aplicação subia normalmente
e quebrava no primeiro uso, um pedaço por vez.

```
ERROR: relation "seguranca_evento" does not exist
Schema validation: missing column [minimum_level] in table [seguranca_acao]
```

Descobrir isso em produção, uma coluna por vez, não era razoável.

## Como funciona

Não há versão, histórico, nem baseline. A rotina compara o **mapeamento real das entidades** com o
que existe no banco e aplica só a diferença, no dialeto em uso.

Isso responde à questão que inviabilizaria um versionamento no estilo Flyway: **os projetos que já
rodam já têm as tabelas de segurança**. Um versionador precisaria saber de que ponto partir, e
erraria. Aqui não há ponto de partida — há comparação. Em banco que já tem tudo, a rotina não faz
nada, e isso é verificado por teste em PostgreSQL e MySQL reais.

O DDL não é escrito à mão por ninguém: é gerado pelo Hibernate a partir das entidades. Uma entidade
que ganha campo passa a ter a coluna correspondente sem que ninguém precise lembrar da migration.

## O que ela nunca faz

Só executa comandos **aditivos**: `create table`, `create sequence`, `create index`,
`alter table ... add column`, e as restrições de tabelas que ela mesma acabou de criar. Qualquer
outra coisa é descartada e registrada no log.

O filtro não é cosmético. Sem ele, o migrador do Hibernate emite `drop constraint` seguido de
`add constraint` para cada chave única **mesmo num banco já completo**, porque não as reconhece com
segurança no catálogo — o que significaria derrubar e recriar as chaves únicas de `seguranca_acao` e
`seguranca_recurso` a cada reinício da aplicação.

Ela também não alcança nada fora do módulo: o mapeamento que ela constrói contém apenas as 15
entidades do `archbase-security`, então as tabelas da aplicação estão fora de alcance por
construção.

## Os limites

**Cria o que falta; não conserta o que está diferente.** Coluna que mudou de tipo, coluna renomeada e
tabela adotada com outro nome continuam exigindo migration escrita por gente. Use
`mode=report` para ver no log o DDL que o Hibernate consideraria necessário.

**Se a estratégia de nomes divergir, ela não escreve.** Antes de aplicar, confere se algum `create
table` se refere a uma tabela que o banco já tem. Se sim, para tudo e registra o erro: seria o mesmo
esquema visto com outro nome, e criar produziria um segundo conjunto de tabelas silenciosamente
vazio enquanto a aplicação continua gravando no primeiro.

**Com mais de um banco, ela pergunta ao Hibernate.** As tabelas de segurança vivem onde as entidades
de segurança estão mapeadas, então o `DataSource` vem do `EntityManagerFactory`. Se ele não o expuser,
vale o `@Primary` — a declaração explícita de quem escreveu a aplicação sobre qual é o banco
principal. Só quando há vários bancos e nenhum `@Primary` a rotina se cala, porque aí não há resposta:
escrever no errado criaria as tabelas onde ninguém vai procurá-las enquanto o banco de verdade segue
sem elas. Nesse caso, marque o principal com `@Primary` ou declare um bean
`ArchbaseSecuritySchemaInitializer` apontando para o `DataSource` correto.

**Falha não derruba a aplicação.** Sem permissão de DDL, a aplicação sobe exatamente como subia
antes desta rotina existir e o que falta aparece no log. É a lição que a trilha de auditoria cobrou
caro: rotina acessória que derruba o serviço principal é pior que a ausência dela. Em homologação,
`fail-on-error=true` transforma isso em falha de subida, para descobrir antes da produção.

## Configuração

```properties
# apply (padrão) | report | off
archbase.security.schema.mode=apply

# false (padrão): problema aqui não impede a aplicação de subir
archbase.security.schema.fail-on-error=false
```

`report` confere e escreve no log o DDL que falta, sem executar nada — útil para revisar antes de
liberar, ou para descobrir divergências que a rotina não sabe corrigir sozinha.

`off` não olha o banco. Para quem controla o esquema por migrations e não quer nem a conferência.

## Relação com o Flyway

O `R__archbase_security_schema.sql` continua existindo e continua valendo para quem usa Flyway com
PostgreSQL. As duas coisas convivem: o que uma já aplicou, a outra não vê como pendente.

A diferença é de alcance. O arquivo Flyway é escrito à mão (e portanto pode ficar fora de sincronia
com as entidades), é específico de PostgreSQL, e só roda para quem tem Flyway ligado. A rotina
descrita aqui não tem nenhuma dessas três limitações.
