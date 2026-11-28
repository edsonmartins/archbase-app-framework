package br.com.archbase.shared.kernel.converters;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import jakarta.persistence.AttributeConverter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Base dos conversores JPA que cifram um atributo de entidade (coluna da tabela).
 *
 * <p><b>Chave.</b> {@code archbase.shared.crypto.key}: Base64 de 16, 24 ou 32 bytes ({@code openssl rand -base64 32}).
 * Lida do {@code Environment} do Spring quando o Hibernate cria o conversor pelo contêiner do Spring (o padrão do
 * Spring Boot); fora do Spring, da propriedade de sistema de mesmo nome ou da variável
 * {@code ARCHBASE_SHARED_CRYPTO_KEY}.
 *
 * <p><b>Formato.</b> Com a chave configurada, grava no formato atual ({@link CifraDeAtributo}: AES-GCM, IV aleatório,
 * prefixo {@code v2:}). Lê os dois formatos: o atual com a chave configurada, e o anterior (Base64 puro) com a chave
 * antiga. Cada linha passa ao formato atual na próxima vez que for gravada.
 *
 * <p><b>Até a 3.5.0</b> a cifra era AES/CBC com IV zerado e a chave {@code "MySuperSecretKey"} fixa no código-fonte:
 * quem tinha o código decifrava a coluna. <b>Sem a chave configurada, o comportamento continua esse</b>, para não
 * tornar ilegível o que já está gravado nem recusar gravações que hoje funcionam; um aviso é registrado no primeiro
 * uso.
 *
 * @author edsonmartins
 */
public abstract class AbstractEncryptDecryptConverter<X>
        implements AttributeConverter<X, String>, EnvironmentAware {

    /** Propriedade da chave. */
    public static final String PROPRIEDADE_CHAVE = "archbase.shared.crypto.key";

    /** Variável de ambiente usada fora do Spring. */
    public static final String VARIAVEL_CHAVE = "ARCHBASE_SHARED_CRYPTO_KEY";

    /**
     * Chave do formato anterior, fixa no código-fonte. Só serve para ler o que já foi gravado e, sem chave
     * configurada, para continuar gravando como antes.
     */
    private static final String CHAVE_LEGADA = "MySuperSecretKey";

    private static final System.Logger LOG = System.getLogger(AbstractEncryptDecryptConverter.class.getName());
    private static final AtomicBoolean AVISOU = new AtomicBoolean();

    /**
     * CipherMaker do formato anterior.
     */
    private final CipherMaker cipherMaker;

    /** Chave configurada; {@code null} enquanto não resolvida. */
    private volatile byte[] chave;
    private volatile boolean chaveResolvida;

    /**
     * Constructor
     *
     * @param cipherMaker
     */
    public AbstractEncryptDecryptConverter(CipherMaker cipherMaker) {
        this.cipherMaker = cipherMaker;
    }

    /**
     * Com a chave informada (16, 24 ou 32 bytes), sem consultar propriedade nem ambiente.
     */
    protected AbstractEncryptDecryptConverter(CipherMaker cipherMaker, byte[] chave) {
        this.cipherMaker = cipherMaker;
        this.chave = chave == null ? null : chave.clone();
        this.chaveResolvida = true;
    }

    /**
     * Constructor padrão
     */
    public AbstractEncryptDecryptConverter() {
        this(new CipherMaker());
    }

    /** Chamado pelo Spring quando é ele quem cria o conversor. */
    @Override
    public void setEnvironment(Environment environment) {
        if (chaveResolvida) {
            return;
        }
        String valor = environment.getProperty(PROPRIEDADE_CHAVE);
        if (valor != null && !valor.isBlank()) {
            chave = CifraDeAtributo.chaveBase64(valor, PROPRIEDADE_CHAVE);
            chaveResolvida = true;
        }
    }

    private byte[] chave() {
        if (!chaveResolvida) {
            String valor = System.getProperty(PROPRIEDADE_CHAVE);
            if (valor == null || valor.isBlank()) {
                valor = System.getenv(VARIAVEL_CHAVE);
            }
            if (valor != null && !valor.isBlank()) {
                chave = CifraDeAtributo.chaveBase64(valor, PROPRIEDADE_CHAVE);
            }
            chaveResolvida = true;
        }
        return chave;
    }

    @Override
    public String convertToDatabaseColumn(X attribute) {
        if (!isNotNullOrEmpty(attribute)) {
            return convertEntityAttributeToString(attribute);
        }
        byte[] chaveAtual = chave();
        if (chaveAtual != null) {
            return CifraDeAtributo.cifrar(chaveAtual, convertEntityAttributeToString(attribute));
        }
        if (AVISOU.compareAndSet(false, true)) {
            LOG.log(System.Logger.Level.WARNING, "{0} sem {1}: o atributo é cifrado com a chave fixa no "
                    + "código-fonte do Archbase, que qualquer um com o código decifra. Configure a chave (Base64 de "
                    + "16, 24 ou 32 bytes); o que já está gravado continua legível.",
                    getClass().getSimpleName(), PROPRIEDADE_CHAVE);
        }
        try {
            Cipher cipher = cipherMaker.configureAndGetInstance(Cipher.ENCRYPT_MODE, CHAVE_LEGADA);
            return encryptData(cipher, attribute);
        } catch (NoSuchAlgorithmException
                 | InvalidKeyException
                 | InvalidAlgorithmParameterException
                 | BadPaddingException
                 | NoSuchPaddingException
                 | IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public X convertToEntityAttribute(String dbData) {
        if (!isNotEmpty(dbData)) {
            return convertStringToEntityAttribute(dbData);
        }
        if (CifraDeAtributo.formatoAtual(dbData)) {
            byte[] chaveAtual = chave();
            if (chaveAtual == null) {
                throw new IllegalStateException("Valor cifrado no formato atual, mas " + PROPRIEDADE_CHAVE
                        + " não está configurada");
            }
            return convertStringToEntityAttribute(CifraDeAtributo.decifrar(chaveAtual, dbData));
        }
        try {
            Cipher cipher = cipherMaker.configureAndGetInstance(Cipher.DECRYPT_MODE, CHAVE_LEGADA);
            return decryptData(cipher, dbData);
        } catch (NoSuchAlgorithmException
                 | InvalidAlgorithmParameterException
                 | InvalidKeyException
                 | NoSuchPaddingException
                 | BadPaddingException
                 | IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A classe concreta que implementa esta classe abstrata terá que
     * fornecer a implementação. Para criptografia de string simples, o
     * a implementação é simples porque o apache commons lang StringUtils pode ser usado.
     * Mas este método foi abstraído porque pode haver outros tipos de nulos
     * técnica de verificação necessária quando uma entidade não String deve ser criptografada
     *
     * @param attribute
     * @return
     */
    abstract boolean isNotNullOrEmpty(X attribute);

    /**
     * A classe concreta que implementa esta classe abstrata terá que
     * fornecer a implementação. Para descriptografar uma string, é simples como um
     * String deve ser retornada, mas para outros tipos não String mais algum código
     * pode ter que ser implementado. Por exemplo, um tipo de data de string de data.
     *
     * @param dbData
     * @return
     */
    abstract X convertStringToEntityAttribute(String dbData);

    /**
     * A classe concreta que implementa esta classe abstrata terá que
     * fornecer a implementação. Para criptografar uma string, é simples como um
     * String deve ser retornada, mas para outros tipos não String mais algum código
     * pode ter que ser implementado. Por exemplo, um tipo de data de string de data.
     *
     * @param attribute
     * @return
     */
    abstract String convertEntityAttributeToString(X attribute);

    /**
     * Método auxiliar para criptografar dados
     *
     * @param cipher
     * @param attribute
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private String encryptData(Cipher cipher, X attribute)
            throws IllegalBlockSizeException, BadPaddingException {
        byte[] bytesToEncrypt = convertEntityAttributeToString(attribute)
                .getBytes();
        byte[] encryptedBytes = cipher.doFinal(bytesToEncrypt);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Método auxiliar para descriptografar dados
     *
     * @param cipher
     * @param dbData
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private X decryptData(Cipher cipher, String dbData)
            throws IllegalBlockSizeException, BadPaddingException {
        byte[] bytesToDecrypt = Base64.getDecoder().decode(dbData);
        byte[] decryptedBytes = cipher.doFinal(bytesToDecrypt);
        return convertStringToEntityAttribute(new String(decryptedBytes));
    }
}