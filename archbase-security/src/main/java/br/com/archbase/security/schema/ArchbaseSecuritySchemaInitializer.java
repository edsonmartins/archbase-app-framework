package br.com.archbase.security.schema;

import br.com.archbase.security.persistence.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerCollectingImpl;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * O framework passa a entregar o próprio esquema de segurança, sozinho, na subida.
 *
 * <p><b>O problema que resolve.</b> Toda versão nova do Archbase pode acrescentar coluna ou tabela
 * às entidades de segurança. Até aqui cada projeto descobria isso em runtime, um pedaço por vez, e
 * pelo pior caminho possível: a aplicação subia e só quebrava no primeiro login
 * ({@code relation "seguranca_evento" does not exist}). O código que exige a coluna e o DDL que a
 * cria moram no mesmo repositório — não faz sentido entregar um e cobrar o outro de quem consome.
 *
 * <p><b>Por que não é Flyway.</b> Existe um {@code R__archbase_security_schema.sql} para quem usa
 * Flyway, e ele continua valendo. Mas ele tem três limites que esta rotina não tem: é escrito à mão
 * e portanto vive fora de sincronia com as entidades; é específico de PostgreSQL; e só roda para
 * quem tem Flyway ligado — o que exclui, por exemplo, quem controla o esquema por outros meios.
 *
 * <p>Aqui o DDL não é escrito por ninguém: é <b>derivado do mapeamento real</b> pelo próprio
 * Hibernate, no dialeto do banco em uso. Uma entidade que ganha campo passa a ter a coluna
 * correspondente sem que ninguém precise lembrar de escrever a migration.
 *
 * <p><b>Por que não precisa de baseline.</b> Foi a ressalva certa: os projetos que já rodam já têm
 * as tabelas de segurança. Um versionamento como o do Flyway precisaria saber "de que ponto partir",
 * e erraria. Esta rotina não tem versão nem histórico — ela <b>compara</b> o mapeamento com o banco
 * e cria só a diferença. Em base que já tem tudo, não faz nada. É idempotente por construção, e não
 * por disciplina de quem escreve o script.
 *
 * <p><b>O que ela nunca faz.</b> Não remove tabela, não remove coluna, não altera tipo de coluna
 * existente e não toca em <b>nada</b> fora das entidades listadas em {@link #ENTIDADES} — o metadata
 * que ela constrói contém apenas essas classes, então as tabelas da aplicação estão fora de alcance
 * por construção, não por promessa.
 *
 * <p><b>O limite honesto.</b> Ela cria o que falta; não conserta o que está diferente. Uma coluna que
 * mudou de tipo, foi renomeada, ou uma tabela adotada com outro nome continuam exigindo migration
 * escrita à mão. Para essas, {@link ArchbaseSecuritySchemaProperties.Mode#REPORT} mostra no log o
 * DDL que o Hibernate consideraria necessário.
 *
 * <p><b>Se falhar, não derruba a aplicação</b> (ver {@code fail-on-error}). É a lição que a trilha de
 * auditoria cobrou caro: uma rotina acessória que derruba o serviço principal é pior do que a
 * ausência dela.
 */
public class ArchbaseSecuritySchemaInitializer implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseSecuritySchemaInitializer.class);

    /**
     * As entidades cujo esquema este módulo entrega.
     *
     * <p>Listadas uma a uma, e não descobertas por varredura de pacote, por dois motivos. O primeiro
     * é que esta lista <b>delimita o que a rotina pode tocar</b>: o que não está aqui não entra no
     * metadata e portanto não pode ser criado nem alterado. O segundo é que uma varredura silenciosa
     * arrastaria para o DDL automático qualquer entidade nova do pacote, inclusive uma que ainda não
     * se pretende publicar.
     *
     * <p>Entidade nova no módulo exige uma linha aqui. É de propósito.
     */
    private static final List<Class<?>> ENTIDADES = List.of(
            AccessIntervalEntity.class,
            AccessScheduleEntity.class,
            AccessTokenEntity.class,
            ActionEntity.class,
            ApiTokenEntity.class,
            ArchbaseSecurityRevision.class,
            GroupEntity.class,
            PasswordResetTokenEntity.class,
            PermissionEntity.class,
            ProfileEntity.class,
            ResourceEntity.class,
            SecurityEntity.class,
            SecurityEventEntity.class,
            UserEntity.class,
            UserGroupEntity.class);

    /**
     * As configurações que precisam ser <b>idênticas</b> às da aplicação.
     *
     * <p>São as que decidem o nome físico de cada tabela e coluna. Se esta rotina montasse o
     * mapeamento com naming diferente do que a aplicação usa, ela não veria as tabelas existentes e
     * criaria um segundo conjunto com outro nome — o pior desfecho possível, porque a aplicação
     * continuaria funcionando enquanto grava em tabelas erradas. Daí serem herdadas do
     * EntityManagerFactory já pronto, e não redeclaradas aqui.
     *
     * <p>A salvaguarda em {@link #divergenciaDeNaming} existe para o caso de a herança não bastar.
     */
    private static final List<String> HERDADAS = List.of(
            "hibernate.dialect",
            "hibernate.physical_naming_strategy",
            "hibernate.implicit_naming_strategy",
            "hibernate.globally_quoted_identifiers",
            "hibernate.globally_quoted_identifiers_skip_column_definitions",
            "hibernate.default_schema",
            "hibernate.default_catalog",
            "hibernate.auto_quote_keyword");

    /**
     * As configurações do Envers vêm inteiras, por prefixo.
     *
     * <p>São elas que decidem o nome das tabelas de auditoria e das colunas de revisão. Herdá-las é
     * o que faz as {@code _AUD} entrarem neste mapeamento exatamente como a aplicação as espera — e,
     * quando a trilha está desligada, é o que faz não entrarem.
     */
    private static final List<String> PREFIXOS_HERDADOS = List.of(
            "org.hibernate.envers.",
            "hibernate.envers.");

    private final ObjectProvider<DataSource> dataSourceProvider;
    private final ObjectProvider<EntityManagerFactory> entityManagerFactory;
    private final ArchbaseSecuritySchemaProperties properties;

    public ArchbaseSecuritySchemaInitializer(ObjectProvider<DataSource> dataSource,
                                            ObjectProvider<EntityManagerFactory> entityManagerFactory,
                                            ArchbaseSecuritySchemaProperties properties) {
        this.dataSourceProvider = dataSource;
        this.entityManagerFactory = entityManagerFactory;
        this.properties = properties;
    }

    /**
     * Roda depois que todos os singletons existem — inclusive o EntityManagerFactory, de quem as
     * configurações de naming são herdadas — e antes de a aplicação aceitar tráfego.
     */
    @Override
    public void afterSingletonsInstantiated() {
        if (properties.getMode() == ArchbaseSecuritySchemaProperties.Mode.OFF) {
            return;
        }
        try {
            conferirAgora();
        } catch (RuntimeException e) {
            if (properties.isFailOnError()) {
                throw e;
            }
            log.error("[archbase-security] Não foi possível conferir o esquema de segurança: {}. "
                    + "A aplicação segue com o esquema como está. Se faltar tabela ou coluna, o erro "
                    + "aparecerá no primeiro uso. Para investigar, use "
                    + "archbase.security.schema.mode=report.", e.toString(), e);
        }
    }

    /**
     * Confere o esquema e devolve o que faltava.
     *
     * <p>Público e com retorno porque o resultado precisa ser <b>verificável</b>: uma rotina de
     * esquema cujo único vestígio é uma linha de log só pode ser testada lendo log, e um teste que lê
     * log passa a valer pelo texto da mensagem em vez de pelo efeito. A lista devolvida é a mesma que
     * foi (ou seria) executada.
     *
     * @return os comandos que faltavam; vazia quando o esquema já estava completo
     */
    public List<String> conferirAgora() {
        DataSource dataSource = dataSourceDaSeguranca();
        if (dataSource == null) {
            return List.of();
        }
        Map<String, Object> settings = settingsHerdadas(dataSource);
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            ENTIDADES.forEach(sources::addAnnotatedClass);
            Metadata metadata = sources.buildMetadata();

            List<String> pendente = apenasAditivos(ddlPendente(registry, metadata, settings));
            if (pendente.isEmpty()) {
                log.debug("[archbase-security] Esquema de segurança já está completo.");
                return pendente;
            }

            Set<String> divergentes = divergenciaDeNaming(pendente, dataSource);
            if (!divergentes.isEmpty()) {
                // Aqui o mapeamento pediu para criar uma tabela que o banco já tem. Isso não é falta
                // de esquema: é o mesmo esquema visto com outro nome. Criar seria produzir um
                // segundo conjunto de tabelas, silenciosamente vazio, enquanto a aplicação continua
                // gravando no primeiro.
                log.error("[archbase-security] O esquema NÃO foi tocado: o mapeamento pediu para criar "
                        + "{}, que já existe(m) no banco. Isso indica que a estratégia de nomes desta "
                        + "rotina difere da usada pela aplicação. Criar produziria tabelas duplicadas. "
                        + "Confira hibernate.physical_naming_strategy e "
                        + "hibernate.globally_quoted_identifiers, ou desligue com "
                        + "archbase.security.schema.mode=off.", divergentes);
                return List.of();
            }

            if (properties.getMode() == ArchbaseSecuritySchemaProperties.Mode.REPORT) {
                log.info("[archbase-security] Faltam {} comando(s) no esquema de segurança. Nada foi "
                        + "executado (mode=report). Para aplicar, use "
                        + "archbase.security.schema.mode=apply:\n{}", pendente.size(), String.join("\n", pendente));
                return pendente;
            }

            log.info("[archbase-security] Completando o esquema de segurança: {} comando(s).", pendente.size());
            executar(pendente, dataSource);
            log.info("[archbase-security] Esquema de segurança em dia.");
            return pendente;
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    /** O DDL que o Hibernate considera necessário. Coleta apenas; nada é executado. */
    private List<String> ddlPendente(StandardServiceRegistry registry, Metadata metadata,
                                     Map<String, Object> settings) {
        List<String> comandos = new ArrayList<>();
        SchemaManagementTool tool = registry.requireService(SchemaManagementTool.class);
        SchemaMigrator migrator = tool.getSchemaMigrator(settings);

        // Coleta em vez de relançar: um comando que falha (falta de permissão numa tabela, índice
        // grande demais para o dialeto) não deve impedir os demais de rodarem.
        ExceptionHandlerCollectingImpl erros = new ExceptionHandlerCollectingImpl();
        ExecutionOptions opcoes = SchemaManagementToolCoordinator.buildExecutionOptions(settings, erros);

        migrator.doMigration(metadata, opcoes, contributed -> true, new TargetDescriptor() {
            @Override
            public EnumSet<TargetType> getTargetTypes() {
                // SCRIPT, sempre: nada é executado aqui. Quem executa é executar(), sobre a lista já
                // filtrada — deixar o migrator aplicar direto aplicaria também o que ele quer
                // derrubar.
                return EnumSet.of(TargetType.SCRIPT);
            }

            @Override
            public ScriptTargetOutput getScriptTargetOutput() {
                return new ScriptTargetOutput() {
                    @Override
                    public void prepare() {
                        // sem recurso a abrir: os comandos ficam na lista acima
                    }

                    @Override
                    public void accept(String comando) {
                        comandos.add(comando);
                    }

                    @Override
                    public void release() {
                        // idem
                    }
                };
            }
        });

        erros.getExceptions().forEach(e ->
                log.warn("[archbase-security] Comando de esquema recusado pelo banco: {}", e.getMessage()));

        return comandos;
    }

    /**
     * Descarta tudo que não seja estritamente aditivo.
     *
     * <p><b>Por que este filtro existe.</b> O migrator do Hibernate não se limita a criar o que
     * falta: num banco <i>já completo</i> ele emite {@code drop constraint} seguido de
     * {@code add constraint} para cada chave única, porque não as reconhece com segurança no
     * catálogo. Aplicado como veio, isto derrubaria e recriaria as chaves únicas de
     * {@code seguranca_acao} e {@code seguranca_recurso} <b>a cada reinício da aplicação</b> — uma
     * janela sem restrição de unicidade em tabela de segurança, mais uma varredura da tabela inteira
     * para recriá-la. Não foi hipótese: é o que o teste de banco completo mostrou.
     *
     * <p>Por lista branca, e não por lista negra de {@code drop}: comando que esta rotina não
     * reconhece é comando que ela não executa. O que sobrar aparece no log e continua sendo trabalho
     * de uma migration escrita por gente.
     *
     * <p>Restrições de tabela recém-criada passam — sem elas a tabela nasceria sem as chaves
     * estrangeiras. Nesse caso não há o que derrubar, porque a tabela não existia.
     */
    private List<String> apenasAditivos(List<String> comandos) {
        Set<String> criadasAgora = new HashSet<>();
        for (String comando : comandos) {
            if (comando.trim().toLowerCase(Locale.ROOT).startsWith("create table ")) {
                criadasAgora.add(nomeDaTabela(comando));
            }
        }

        List<String> mantidos = new ArrayList<>();
        List<String> descartados = new ArrayList<>();
        for (String comando : comandos) {
            if (ehAditivo(comando, criadasAgora)) {
                mantidos.add(comando);
            } else {
                descartados.add(comando);
            }
        }

        if (!descartados.isEmpty()) {
            log.debug("[archbase-security] {} comando(s) de esquema ignorados por não serem aditivos: {}",
                    descartados.size(), descartados);
        }
        return mantidos;
    }

    private boolean ehAditivo(String comando, Set<String> criadasAgora) {
        String c = comando.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        if (c.startsWith("create table ") || c.startsWith("create sequence ")
                || c.startsWith("create index ") || c.startsWith("create unique index ")) {
            return true;
        }
        if (!c.startsWith("alter table ")) {
            return false;
        }
        if (c.contains(" drop ")) {
            return false;
        }
        if (c.contains(" add column ") || c.contains(" add if not exists ")) {
            return true;
        }
        if (c.contains(" add constraint ")) {
            // Só para tabela que esta mesma passada está criando: aí a restrição faz parte do
            // nascimento da tabela, e não de uma alteração no que já estava em uso.
            return criadasAgora.contains(alvoDoAlterTable(comando));
        }
        // "alter table X add colunaSemPalavraColumn ..." — alguns dialetos omitem "column".
        return c.contains(" add ");
    }

    private String alvoDoAlterTable(String alterTable) {
        String resto = alterTable.trim().substring("alter table ".length()).trim();
        if (resto.toLowerCase(Locale.ROOT).startsWith("if exists ")) {
            resto = resto.substring("if exists ".length()).trim();
        }
        int fim = resto.indexOf(' ');
        String nome = (fim < 0 ? resto : resto.substring(0, fim))
                .replace("\"", "").replace("`", "").replace("[", "").replace("]", "");
        int ponto = nome.lastIndexOf('.');
        return (ponto >= 0 ? nome.substring(ponto + 1) : nome).toLowerCase(Locale.ROOT);
    }

    /**
     * Executa os comandos, um por um, cada um em sua própria transação.
     *
     * <p>{@code autoCommit} ligado não é detalhe: no PostgreSQL, um erro dentro de uma transação
     * aborta a transação inteira e faz o banco recusar todo comando seguinte. Sem isolar cada
     * comando, uma única falha — um índice grande demais para o dialeto, por exemplo — levaria junto
     * todos os que viessem depois. Foi assim que a trilha de auditoria derrubou o login.
     *
     * <p>Uma falha é registrada e a execução segue: criar quatro das cinco tabelas que faltam é
     * melhor do que criar nenhuma.
     */
    private void executar(List<String> comandos, DataSource dataSource) {
        try (Connection conexao = dataSource.getConnection()) {
            boolean autoCommitAnterior = conexao.getAutoCommit();
            conexao.setAutoCommit(true);
            try {
                for (String comando : comandos) {
                    String sql = comando.trim();
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1);
                    }
                    try (Statement statement = conexao.createStatement()) {
                        statement.execute(sql);
                    } catch (SQLException e) {
                        log.warn("[archbase-security] Comando de esquema recusado pelo banco: {} — {}",
                                sql, e.getMessage());
                    }
                }
            } finally {
                conexao.setAutoCommit(autoCommitAnterior);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("não foi possível aplicar o esquema de segurança", e);
        }
    }

    /**
     * Tabelas que o mapeamento quer criar e que o banco já tem.
     *
     * <p>Lista vazia é o caso normal. Qualquer coisa aqui significa que os dois lados chamam a mesma
     * tabela por nomes diferentes, e nesse caso o certo é não escrever nada.
     */
    private Set<String> divergenciaDeNaming(List<String> comandos, DataSource dataSource) {
        Set<String> pedidas = new HashSet<>();
        for (String comando : comandos) {
            String normalizado = comando.trim().toLowerCase(Locale.ROOT);
            if (normalizado.startsWith("create table ")) {
                pedidas.add(nomeDaTabela(comando));
            }
        }
        if (pedidas.isEmpty()) {
            return Set.of();
        }

        Set<String> existentes = tabelasDoBanco(dataSource);
        pedidas.retainAll(existentes);
        return pedidas;
    }

    private String nomeDaTabela(String createTable) {
        String resto = createTable.trim().substring("create table ".length()).trim();
        if (resto.toLowerCase(Locale.ROOT).startsWith("if not exists ")) {
            resto = resto.substring("if not exists ".length()).trim();
        }
        int fim = resto.length();
        for (int i = 0; i < resto.length(); i++) {
            char c = resto.charAt(i);
            if (c == '(' || Character.isWhitespace(c)) {
                fim = i;
                break;
            }
        }
        // Sem aspas e sem qualificação de schema: a comparação é pelo nome simples, porque é assim
        // que o catálogo do banco devolve.
        String nome = resto.substring(0, fim).replace("\"", "").replace("`", "").replace("[", "").replace("]", "");
        int ponto = nome.lastIndexOf('.');
        return (ponto >= 0 ? nome.substring(ponto + 1) : nome).toLowerCase(Locale.ROOT);
    }

    private Set<String> tabelasDoBanco(DataSource dataSource) {
        Set<String> nomes = new HashSet<>();
        try (Connection conexao = dataSource.getConnection()) {
            DatabaseMetaData meta = conexao.getMetaData();
            try (ResultSet rs = meta.getTables(conexao.getCatalog(), conexao.getSchema(), "%",
                    new String[]{"TABLE"})) {
                while (rs.next()) {
                    nomes.add(rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception e) {
            // Sem o catálogo não dá para afirmar divergência. Devolver vazio deixa a rotina seguir —
            // o Hibernate ainda usa "if not exists" onde o dialeto suporta.
            log.debug("[archbase-security] Não foi possível ler o catálogo do banco: {}", e.toString());
        }
        return nomes;
    }

    /**
     * O banco onde vivem as tabelas de segurança.
     *
     * <p><b>Perguntado ao EntityManagerFactory antes de qualquer outra coisa</b>, porque é ele quem
     * define a resposta: as tabelas de segurança vivem onde as entidades de segurança estão
     * mapeadas. Contar beans {@code DataSource} responderia a outra pergunta — "quantos bancos esta
     * aplicação tem" — que não é a mesma e leva ao erro justamente nos casos que importam.
     *
     * <p>Aplicações com mais de um banco são comuns (um relacional principal e um legado somente de
     * leitura, por exemplo), e a primeira versão desta rotina simplesmente desistia quando havia mais
     * de um candidato. Ela ficava inerte exatamente onde era mais necessária, e sem alarde nenhum.
     *
     * <p>Se o EntityManagerFactory não expuser o DataSource, sobra o {@code @Primary} — que é a
     * declaração explícita de quem escreveu a aplicação sobre qual é o banco principal. Sem nada
     * disso, a rotina se cala: escrever no banco errado é pior do que não escrever, porque criaria as
     * tabelas onde ninguém vai procurá-las enquanto o banco de verdade segue sem elas.
     *
     * <p>{@code bancoEscolhido()} existe para o teste alcançar esta decisão: ela é a mais fácil de
     * errar em silêncio, porque errada não dá exceção nenhuma — só deixa de fazer o que prometeu.
     */
    DataSource bancoEscolhido() {
        return dataSourceDaSeguranca();
    }

    private DataSource dataSourceDaSeguranca() {
        DataSource doMapeamento = dataSourceDoEntityManagerFactory();
        if (doMapeamento != null) {
            return doMapeamento;
        }

        DataSource unicoOuPrimario = dataSourceProvider.getIfUnique();
        if (unicoOuPrimario != null) {
            return unicoOuPrimario;
        }

        long candidatos = dataSourceProvider.stream().count();
        if (candidatos == 0) {
            log.debug("[archbase-security] Sem DataSource; o esquema de segurança não foi conferido.");
        } else {
            log.info("[archbase-security] {} DataSources e nenhum marcado como @Primary: não dá para "
                    + "saber em qual vivem as tabelas de segurança, e o esquema não foi conferido. "
                    + "Marque o principal com @Primary ou declare um bean "
                    + "ArchbaseSecuritySchemaInitializer apontando para o DataSource correto.", candidatos);
        }
        return null;
    }

    /**
     * O DataSource que o próprio Hibernate está usando para as entidades mapeadas.
     *
     * <p>Envolto em try/catch largo de propósito: é uma navegação por dentro do Hibernate, e nem todo
     * arranjo a suporta (uma unidade de persistência sem DataSource, um provedor de conexão que não
     * se deixa desembrulhar). Falhar aqui não é erro — é sinal de que a resposta vem do
     * {@code @Primary}.
     */
    private DataSource dataSourceDoEntityManagerFactory() {
        EntityManagerFactory emf = entityManagerFactory.getIfAvailable();
        if (emf == null) {
            return null;
        }
        try {
            SessionFactoryImplementor sessionFactory = emf.unwrap(SessionFactoryImplementor.class);
            ConnectionProvider provider = sessionFactory.getServiceRegistry().getService(ConnectionProvider.class);
            if (provider != null && provider.isUnwrappableAs(DataSource.class)) {
                return provider.unwrap(DataSource.class);
            }
        } catch (Exception e) {
            log.debug("[archbase-security] O EntityManagerFactory não expôs o DataSource ({}); "
                    + "usando o @Primary.", e.toString());
        }
        return null;
    }

    /**
     * As configurações da aplicação, mais o DataSource dela.
     *
     * <p>A ação fica em {@code none}: quem decide o que fazer é esta classe, chamando o migrator
     * diretamente. Deixar a ação no mapa faria o Hibernate agir sozinho ao construir o registry.
     */
    private Map<String, Object> settingsHerdadas(DataSource dataSource) {
        Map<String, Object> settings = new HashMap<>();

        EntityManagerFactory emf = entityManagerFactory.getIfAvailable();
        if (emf != null) {
            Map<String, Object> daAplicacao = emf.getProperties();
            for (String chave : HERDADAS) {
                Object valor = daAplicacao.get(chave);
                if (valor != null) {
                    settings.put(chave, valor);
                }
            }
            daAplicacao.forEach((chave, valor) -> {
                if (valor != null && PREFIXOS_HERDADOS.stream().anyMatch(chave::startsWith)) {
                    settings.put(chave, valor);
                }
            });
        } else {
            log.debug("[archbase-security] Sem EntityManagerFactory disponível; usando os padrões de "
                    + "nomes do Hibernate.");
        }

        settings.put("hibernate.connection.datasource", dataSource);
        settings.put("hibernate.hbm2ddl.auto", "none");
        settings.put("jakarta.persistence.schema-generation.database.action", "none");
        return settings;
    }
}
