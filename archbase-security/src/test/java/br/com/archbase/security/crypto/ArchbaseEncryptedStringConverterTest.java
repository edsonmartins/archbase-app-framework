package br.com.archbase.security.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Cobre o {@link ArchbaseEncryptedStringConverter}: grava sempre em AES-GCM com prefixo {@code gcm:}
 * e faz leitura mista (GCM novo + texto puro legado sem prefixo).
 */
class ArchbaseEncryptedStringConverterTest {

    private static final String KEY = "0123456789abcdef0123456789abcdef"; // 32 chars = 256 bits
    private final ArchbaseEncryptedStringConverter converter =
            new ArchbaseEncryptedStringConverter(new ArchbaseCryptoService(KEY));

    @Test
    void gravaEmGcmComPrefixoELeDeVolta() {
        String plain = "segredo-do-cliente";
        String stored = converter.convertToDatabaseColumn(plain);
        assertTrue(stored.startsWith("gcm:"), "valor novo deve ser marcado como GCM");
        assertEquals(plain, converter.convertToEntityAttribute(stored));
    }

    @Test
    void leTextoPuroLegadoSemPrefixoComoEsta() {
        String legadoPlano = "valor-em-texto-puro";
        assertEquals(legadoPlano, converter.convertToEntityAttribute(legadoPlano));
    }

    @Test
    void nullEntraNullSai() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
