package br.com.archbase.security.crypto;

import jakarta.persistence.AttributeConverter;
import org.springframework.stereotype.Component;

/**
 * Converter JPA reutilizável para cifrar campos String em repouso com AES-GCM.
 *
 * <p>Uso: anote o campo com {@code @Convert(converter = ArchbaseEncryptedStringConverter.class)}.
 * Escreve sempre cifrado (AES-GCM via {@link ArchbaseCryptoService}), marcado com o prefixo
 * {@value #GCM_PREFIX} (versão de esquema, sem coluna extra). Na leitura faz <b>leitura mista</b>:
 * valores com o prefixo são decifrados; valores sem prefixo são tratados como <b>texto puro
 * legado</b> (pré-migração) e retornados como estão — o que permite ligar a cifragem a um campo
 * antes plaintext sem quebrar a leitura, enquanto a migração em massa converte o restante.
 *
 * <p>Para campos cujo legado é outro algoritmo (ex.: AES-ECB), migre os dados para GCM antes de
 * usar este converter, ou use um converter específico com a leitura legada apropriada.
 */
@Component
public class ArchbaseEncryptedStringConverter implements AttributeConverter<String, String> {

    /** Marcador de versão para valores cifrados em AES-GCM. */
    public static final String GCM_PREFIX = "gcm:";

    private final ArchbaseCryptoService cryptoService;

    public ArchbaseEncryptedStringConverter(ArchbaseCryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return GCM_PREFIX + cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData.startsWith(GCM_PREFIX)) {
            return cryptoService.decrypt(dbData.substring(GCM_PREFIX.length()));
        }
        return dbData; // texto puro legado (ainda não migrado)
    }
}
