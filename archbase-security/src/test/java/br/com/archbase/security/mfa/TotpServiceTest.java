package br.com.archbase.security.mfa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TOTP (RFC 6238) — vetores oficiais do Apêndice B (SHA1) e propriedades de
 * geração/validação (round-trip, janela de tolerância, rejeição de código errado).
 */
@DisplayName("TotpService — RFC 6238")
class TotpServiceTest {

    private final TotpService totp = new TotpService();

    // Seed de teste da RFC 6238 (ASCII "12345678901234567890") em Base32.
    private final String secret = TotpService.base32Encode("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test
    @DisplayName("vetores oficiais da RFC 6238 (SHA1, 8 dígitos)")
    void vetoresRfc() {
        assertThat(totp.generateCode(secret, 59_000L, 8, 30)).isEqualTo("94287082");
        assertThat(totp.generateCode(secret, 1_111_111_109_000L, 8, 30)).isEqualTo("07081804");
        assertThat(totp.generateCode(secret, 1_111_111_111_000L, 8, 30)).isEqualTo("14050471");
        assertThat(totp.generateCode(secret, 1_234_567_890_000L, 8, 30)).isEqualTo("89005924");
        assertThat(totp.generateCode(secret, 2_000_000_000_000L, 8, 30)).isEqualTo("69279037");
    }

    @Test
    @DisplayName("Base32 faz round-trip dos bytes")
    void base32RoundTrip() {
        byte[] original = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        assertThat(TotpService.base32Decode(TotpService.base32Encode(original))).isEqualTo(original);
    }

    @Test
    @DisplayName("segredo gerado tem 32 chars Base32 (160 bits) e valida o próprio código")
    void geraEValidaProprioCodigo() {
        String s = totp.generateSecret();
        assertThat(s).hasSize(32).matches("[A-Z2-7]+");

        long agora = 1_700_000_000_000L;
        String codigo = totp.generateCode(s, agora);
        assertThat(totp.verifyCode(s, codigo, agora, 1)).isTrue();
    }

    @Test
    @DisplayName("aceita código do passo anterior (skew) mas rejeita fora da janela e código errado")
    void janelaEErro() {
        long agora = 1_700_000_000_000L;
        String codigoPassoAnterior = totp.generateCode(secret, agora - 30_000L);

        assertThat(totp.verifyCode(secret, codigoPassoAnterior, agora, 1)).isTrue();  // ±1 passo
        assertThat(totp.verifyCode(secret, codigoPassoAnterior, agora, 0)).isFalse(); // sem janela
        assertThat(totp.verifyCode(secret, totp.generateCode(secret, agora - 120_000L), agora, 1)).isFalse();
        assertThat(totp.verifyCode(secret, "000000", agora, 1)).isFalse();
        assertThat(totp.verifyCode(secret, null, agora, 1)).isFalse();
    }

    @Test
    @DisplayName("URI de provisionamento no formato otpauth:// com segredo e emissor")
    void provisioningUri() {
        String uri = totp.provisioningUri("ABCDEF", "user@vendax.com.br", "VendaX CRM");
        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("secret=ABCDEF").contains("issuer=VendaX+CRM").contains("period=30");
    }
}
