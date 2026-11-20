package br.com.archbase.ddd.infraestructure.aspect;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Conferência do base64 de data URL antes da decodificação (CVE-2026-114938).
 */
class ArchbaseStorageFieldAspectBase64Test {

    private static final long LIMITE = 1024;

    private static void aceita(String data) {
        assertDoesNotThrow(() -> ArchbaseStorageFieldAspect.requireCanonicalBase64(data, LIMITE));
    }

    private static void recusa(String data) {
        assertThrows(IllegalArgumentException.class,
                () -> ArchbaseStorageFieldAspect.requireCanonicalBase64(data, LIMITE));
    }

    @Test
    void aceitaBase64CanonicoComESemPadding() {
        aceita(Base64.getEncoder().encodeToString("abc".getBytes(StandardCharsets.UTF_8)));   // sem padding
        aceita(Base64.getEncoder().encodeToString("abcd".getBytes(StandardCharsets.UTF_8)));  // ==
        aceita(Base64.getEncoder().encodeToString("abcde".getBytes(StandardCharsets.UTF_8))); // =
        aceita(Base64.getEncoder().encodeToString(new byte[]{(byte) 0xfb, (byte) 0xff, (byte) 0xbf})); // + e /
    }

    @Test
    void recusaFormaDoGatilhoPublicado() {
        recusa("A".repeat(64) + "=");
    }

    @Test
    void recusaPaddingForaDoFim() {
        recusa("AB=C");
        recusa("A===");
        recusa("QQ==QQ==");
    }

    @Test
    void recusaComprimentoQueNaoEMultiploDeQuatro() {
        recusa("");
        recusa("QUJD" + "Q");
        recusa("QUJDRA=");
    }

    @Test
    void recusaCaracteresForaDoAlfabeto() {
        recusa("QUJD\nRA==");
        recusa("QUJD RA==");
        recusa("QUJD-A__"); // base64url não é aceito em data URL
    }

    @Test
    void respeitaOLimiteDeTamanho() {
        aceita(Base64.getEncoder().encodeToString(new byte[(int) LIMITE]));
        recusa(Base64.getEncoder().encodeToString(new byte[(int) LIMITE + 1]));
    }
}
