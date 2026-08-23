package br.com.archbase.security.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Renomeia as colunas de revisão das tabelas {@code _AUD} criadas com a nomenclatura padrão do
 * Envers.
 *
 * <p><b>O defeito que isto conserta.</b> Até a 3.1.16, a rotina de esquema montava o próprio
 * mapeamento sem herdar {@code hibernate.integration.envers.enabled} — então, mesmo com a trilha de
 * auditoria <b>desligada</b>, o Envers seguia ativo ali e ela <b>criava</b> as tabelas {@code _AUD}.
 * E como as propriedades {@code revision_field_name}/{@code revision_type_field_name} só são
 * aplicadas no ramo "trilha ligada", essas tabelas nasceram com o padrão do Envers — {@code rev} e
 * {@code revtype} — e não com {@code id_revisao}/{@code tp_revisao}, que é o que a aplicação usa
 * quando a trilha é ligada.
 *
 * <p>O resultado aparece só no dia em que alguém liga a trilha, e aparece como crash-loop:
 *
 * <pre>
 * ERROR: null value in column "rev" of relation "seguranca_recurso_aud"
 * violates not-null constraint (SQLSTATE 23502)
 * </pre>
 *
 * <p>O Envers passa a gravar {@code id_revisao} e o {@code rev} legado — {@code NOT NULL}, órfão —
 * recusa todo insert.
 *
 * <p><b>Por que a rotina de esquema não resolvia sozinha.</b> Ela é aditiva por construção: vendo
 * que falta {@code id_revisao}, emite {@code ADD COLUMN} e segue. Adicionar não desfaz o
 * {@code rev} legado, e é justamente a convivência dos dois que quebra. Renomear é migração, não
 * adição — precisa de um passo próprio, que é este.
 *
 * <p><b>Preserva o histórico.</b> {@code RENAME COLUMN} mantém os dados: as revisões já gravadas
 * continuam lá, apenas sob o nome novo. Descartar e recriar as tabelas seria mais simples e apagaria
 * exatamente aquilo que uma trilha de auditoria existe para guardar.
 */
class ArchbaseRevisionColumnMigrator {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseRevisionColumnMigrator.class);

    /** Como o Envers nomeia por padrão, e como o Archbase nomeia. */
    private static final String REV_LEGADO = "rev";
    private static final String REV_ARCHBASE = "id_revisao";
    private static final String TIPO_LEGADO = "revtype";
    private static final String TIPO_ARCHBASE = "tp_revisao";

    private final DataSource dataSource;

    ArchbaseRevisionColumnMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Renomeia o que precisar ser renomeado.
     *
     * @param aplicar {@code false} apenas relata, sem tocar no banco
     * @return os comandos necessários; vazia quando não há nada a migrar
     */
    List<String> migrar(boolean aplicar) {
        List<String> comandos = new ArrayList<>();

        for (String tabela : tabelasAud()) {
            Set<String> colunas = colunasDe(tabela);
            comandos.addAll(renomeacoesPara(tabela, colunas));
        }

        if (comandos.isEmpty()) {
            return comandos;
        }

        if (!aplicar) {
            log.warn("[archbase-security] {} tabela(s) de auditoria estão com as colunas de revisão no "
                    + "padrão do Envers (rev/revtype) e a aplicação usa id_revisao/tp_revisao. Enquanto "
                    + "isso não for migrado, LIGAR a trilha derruba a aplicação no primeiro registro. "
                    + "Nada foi executado (mode=report). Comandos necessários:\n{}",
                    comandos.size() / 2, String.join("\n", comandos));
            return comandos;
        }

        log.warn("[archbase-security] Migrando as colunas de revisão de {} comando(s): as tabelas de "
                + "auditoria foram criadas com o padrão do Envers (rev/revtype) por uma versão anterior "
                + "do framework. As linhas já gravadas são preservadas — é RENAME, não recriação.",
                comandos.size());
        executar(comandos);
        return comandos;
    }

    /**
     * As renomeações de uma tabela, se ela estiver no formato legado.
     *
     * <p>Só renomeia quando o nome legado existe <b>e</b> o do Archbase ainda não. Se os dois
     * estiverem presentes, renomear falharia — e esse caso já é o estado quebrado que motivou esta
     * classe, tratado à parte por quem chama.
     */
    private List<String> renomeacoesPara(String tabela, Set<String> colunas) {
        List<String> comandos = new ArrayList<>();
        if (colunas.contains(REV_LEGADO) && !colunas.contains(REV_ARCHBASE)) {
            comandos.add("alter table " + tabela + " rename column " + REV_LEGADO + " to " + REV_ARCHBASE);
        }
        if (colunas.contains(TIPO_LEGADO) && !colunas.contains(TIPO_ARCHBASE)) {
            comandos.add("alter table " + tabela + " rename column " + TIPO_LEGADO + " to " + TIPO_ARCHBASE);
        }
        return comandos;
    }

    /**
     * Tabelas em que as duas nomenclaturas convivem — o estado quebrado.
     *
     * <p>É o que sobra quando uma versão aditiva já acrescentou {@code id_revisao} sem remover o
     * {@code rev}. Renomear não resolve (o destino existe), e a rotina não pode escolher sozinha qual
     * coluna descartar: uma delas tem o histórico. Precisa de decisão humana.
     */
    List<String> tabelasComAsDuasNomenclaturas() {
        List<String> quebradas = new ArrayList<>();
        for (String tabela : tabelasAud()) {
            Set<String> colunas = colunasDe(tabela);
            if (colunas.contains(REV_LEGADO) && colunas.contains(REV_ARCHBASE)) {
                quebradas.add(tabela);
            }
        }
        return quebradas;
    }

    private Set<String> tabelasAud() {
        Set<String> nomes = new LinkedHashSet<>();
        try (Connection conexao = dataSource.getConnection()) {
            DatabaseMetaData meta = conexao.getMetaData();
            try (ResultSet rs = meta.getTables(conexao.getCatalog(), conexao.getSchema(), "%",
                    new String[]{"TABLE"})) {
                while (rs.next()) {
                    String nome = rs.getString("TABLE_NAME");
                    String minusculo = nome.toLowerCase(Locale.ROOT);
                    // Só as do módulo de segurança: uma _AUD da aplicação não é problema desta rotina.
                    if (minusculo.startsWith("seguranca") && minusculo.endsWith("_aud")) {
                        nomes.add(nome);
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("[archbase-security] Não foi possível listar as tabelas de auditoria: {}", e.toString());
        }
        return nomes;
    }

    private Set<String> colunasDe(String tabela) {
        Set<String> colunas = new LinkedHashSet<>();
        try (Connection conexao = dataSource.getConnection()) {
            DatabaseMetaData meta = conexao.getMetaData();
            try (ResultSet rs = meta.getColumns(conexao.getCatalog(), conexao.getSchema(), tabela, "%")) {
                while (rs.next()) {
                    colunas.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
        } catch (SQLException e) {
            log.debug("[archbase-security] Não foi possível ler as colunas de {}: {}", tabela, e.toString());
        }
        return colunas;
    }

    /**
     * Executa cada comando em sua própria transação.
     *
     * <p>Mesma razão do resto da rotina: no PostgreSQL um erro aborta a transação inteira e faz o
     * banco recusar todo comando seguinte. Isolando, uma tabela que falhe não impede as outras.
     */
    private void executar(List<String> comandos) {
        try (Connection conexao = dataSource.getConnection()) {
            boolean anterior = conexao.getAutoCommit();
            conexao.setAutoCommit(true);
            try {
                for (String comando : comandos) {
                    try (Statement statement = conexao.createStatement()) {
                        statement.execute(comando);
                        log.info("[archbase-security] {}", comando);
                    } catch (SQLException e) {
                        log.error("[archbase-security] Falhou ao migrar coluna de revisão: {} — {}",
                                comando, e.getMessage());
                    }
                }
            } finally {
                conexao.setAutoCommit(anterior);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("não foi possível migrar as colunas de revisão", e);
        }
    }
}
