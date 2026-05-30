package br.com.archbase.security.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Cobre o {@link ArchbaseColumnReencryptor}: re-cifra texto puro -> GCM, é idempotente (pula linhas
 * já {@code gcm:}) e ignora tabelas inexistentes.
 */
class ArchbaseColumnReencryptorTest {

    private static final String KEY = "0123456789abcdef0123456789abcdef";

    private final ArchbaseCryptoService crypto = new ArchbaseCryptoService(KEY);
    private final ArchbaseColumnReencryptor reencryptor = new ArchbaseColumnReencryptor(crypto);

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:reenc;DB_CLOSE_DELAY=-1");
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE T_SECRET (ID VARCHAR(50) PRIMARY KEY, VALOR VARCHAR(2000))");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        }
        connection.close();
    }

    @Test
    void recifraTextoPuroPulandoLinhasJaGcm() throws SQLException {
        insert("1", "plain-one");
        insert("2", "plain-two");
        String alreadyGcm = "gcm:" + crypto.encrypt("ja-cifrado");
        insert("3", alreadyGcm);
        insert("4", null); // nulo é ignorado

        int updated = reencryptor.reencryptColumn(connection, "T_SECRET", "ID", "VALOR",
                ArchbaseColumnReencryptor.PLAINTEXT);

        assertEquals(2, updated, "apenas as duas linhas em texto puro devem ser re-cifradas");

        Map<String, String> rows = readAll();
        assertTrue(rows.get("1").startsWith("gcm:"));
        assertTrue(rows.get("2").startsWith("gcm:"));
        assertEquals("plain-one", crypto.decrypt(rows.get("1").substring(4)));
        assertEquals("plain-two", crypto.decrypt(rows.get("2").substring(4)));
        assertEquals(alreadyGcm, rows.get("3"), "linha já GCM deve permanecer intacta");

        // Idempotência: segunda passada não altera nada.
        int again = reencryptor.reencryptColumn(connection, "T_SECRET", "ID", "VALOR",
                ArchbaseColumnReencryptor.PLAINTEXT);
        assertEquals(0, again);
    }

    @Test
    void tabelaInexistenteNaoFalha() throws SQLException {
        int updated = reencryptor.reencryptColumn(connection, "NAO_EXISTE", "ID", "VALOR",
                ArchbaseColumnReencryptor.PLAINTEXT);
        assertEquals(0, updated);
    }

    @Test
    void aplicaDecoderLegadoAntesDeCifrar() throws SQLException {
        // Decoder legado fictício: o valor armazenado é "RAW:" + claro; o decoder remove o prefixo.
        insert("1", "RAW:senha-secreta");

        int updated = reencryptor.reencryptColumn(connection, "T_SECRET", "ID", "VALOR",
                value -> value.substring("RAW:".length()));

        assertEquals(1, updated);
        String stored = readAll().get("1");
        assertTrue(stored.startsWith("gcm:"));
        assertEquals("senha-secreta", crypto.decrypt(stored.substring(4)));
        assertFalse(stored.contains("RAW:"));
    }

    private void insert(String id, String valor) throws SQLException {
        try (var ps = connection.prepareStatement("INSERT INTO T_SECRET (ID, VALOR) VALUES (?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, valor);
            ps.executeUpdate();
        }
    }

    private Map<String, String> readAll() throws SQLException {
        Map<String, String> rows = new HashMap<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT ID, VALOR FROM T_SECRET")) {
            while (rs.next()) {
                rows.put(rs.getString(1), rs.getString(2));
            }
        }
        return rows;
    }
}
