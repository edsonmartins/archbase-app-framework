package br.com.archbase.shared.kernel.converters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cifra de atributo: formato atual e leitura do anterior")
class CifraDeAtributoTest {

    private static final byte[] CHAVE = Base64.getDecoder().decode("q83vEi7uBQ0hT2bSFZ4LqgnFsx2W0Ckzm1vy3pA9Rjc=");

    /** Exatamente o que a 3.5.0 gravava: AES/CBC, IV zerado, chave fixa. */
    private static String formatoAnterior(String texto) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec("MySuperSecretKey".getBytes(), "AES"),
                new IvParameterSpec(new byte[16]));
        return Base64.getEncoder().encodeToString(cipher.doFinal(texto.getBytes()));
    }

    /** Exatamente o que o AttributeEncryptor gravava: AES/ECB. */
    private static String formatoAnteriorEcb(String chave, String texto) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(chave.getBytes(), "AES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal(texto.getBytes()));
    }

    @Test
    @DisplayName("com chave: grava v2 (GCM, IV aleatório) e lê de volta; o mesmo texto dá valores diferentes")
    void comChave() {
        var conversor = new StringEncryptDecryptConverter(CHAVE);
        String a = conversor.convertToDatabaseColumn("123.456.789-09");
        String b = conversor.convertToDatabaseColumn("123.456.789-09");

        assertThat(a).startsWith("v2:").isNotEqualTo(b);
        assertThat(conversor.convertToEntityAttribute(a)).isEqualTo("123.456.789-09");
        assertThat(conversor.convertToEntityAttribute(b)).isEqualTo("123.456.789-09");
    }

    @Test
    @DisplayName("com chave: o que já estava gravado no formato anterior continua legível")
    void legadoLegivel() throws Exception {
        var conversor = new StringEncryptDecryptConverter(CHAVE);
        assertThat(conversor.convertToEntityAttribute(formatoAnterior("segredo antigo"))).isEqualTo("segredo antigo");

        var datas = new LocalDateEncryptDecryptConverter(CHAVE);
        assertThat(datas.convertToEntityAttribute(formatoAnterior("2020-02-29"))).isEqualTo(LocalDate.of(2020, 2, 29));
        assertThat(datas.convertToEntityAttribute(datas.convertToDatabaseColumn(LocalDate.of(2026, 11, 25))))
                .isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    @DisplayName("sem chave: grava como antes (compatibilidade) e lê os dois formatos que dá para ler")
    void semChave() throws Exception {
        var conversor = new StringEncryptDecryptConverter((byte[]) null);
        String gravado = conversor.convertToDatabaseColumn("texto");

        assertThat(gravado).isEqualTo(formatoAnterior("texto"));
        assertThat(conversor.convertToEntityAttribute(gravado)).isEqualTo("texto");

        String atual = new StringEncryptDecryptConverter(CHAVE).convertToDatabaseColumn("texto");
        assertThatThrownBy(() -> conversor.convertToEntityAttribute(atual))
                .hasMessageContaining("archbase.shared.crypto.key");
    }

    @Test
    @DisplayName("valor adulterado ou chave errada é recusado, não decifrado em lixo")
    void adulterado() {
        var conversor = new StringEncryptDecryptConverter(CHAVE);
        String gravado = conversor.convertToDatabaseColumn("valor");
        byte[] bytes = Base64.getDecoder().decode(gravado.substring(3));
        bytes[bytes.length - 1] ^= 1;
        String adulterado = "v2:" + Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> conversor.convertToEntityAttribute(adulterado))
                .isInstanceOf(IllegalStateException.class);
        byte[] outra = CHAVE.clone();
        outra[0] ^= 1;
        assertThatThrownBy(() -> new StringEncryptDecryptConverter(outra).convertToEntityAttribute(gravado))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("chave em Base64 de tamanho errado é recusada com a origem na mensagem")
    void chaveInvalida() {
        String curta = Base64.getEncoder().encodeToString("curta".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> CifraDeAtributo.chaveBase64(curta, "archbase.shared.crypto.key"))
                .hasMessageContaining("archbase.shared.crypto.key").hasMessageContaining("16, 24 ou 32");
    }

    @Test
    @DisplayName("AttributeEncryptor: grava v2, lê o ECB anterior, e aguenta uso concorrente")
    void attributeEncryptor() throws Exception {
        String chave = "0123456789abcdef0123456789abcdef";
        var encryptor = new AttributeEncryptor(chave);

        assertThat(encryptor.convertToEntityAttribute(formatoAnteriorEcb(chave, "antigo"))).isEqualTo("antigo");
        String novo = encryptor.convertToDatabaseColumn("novo");
        assertThat(novo).startsWith("v2:");
        assertThat(encryptor.convertToEntityAttribute(novo)).isEqualTo("novo");

        // Antes um único Cipher era compartilhado entre threads.
        List<String> lidos = IntStream.range(0, 2000).parallel()
                .mapToObj(i -> encryptor.convertToEntityAttribute(encryptor.convertToDatabaseColumn("v" + i)))
                .toList();
        assertThat(lidos).containsExactlyElementsOf(IntStream.range(0, 2000).mapToObj(i -> "v" + i).toList());
    }

    @Test
    @DisplayName("AttributeEncryptor sem a propriedade: erro claro no uso, não na subida")
    void attributeEncryptorSemChave() {
        assertThatThrownBy(() -> new AttributeEncryptor("").convertToDatabaseColumn("x"))
                .hasMessageContaining("aes.encryption.key");
    }
}
