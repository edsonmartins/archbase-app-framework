package br.com.archbase.starter.security.auto.configuration;

import br.com.archbase.security.crypto.ArchbaseColumnReencryptor;
import br.com.archbase.security.crypto.ArchbaseCryptoService;
import br.com.archbase.security.crypto.ArchbaseEncryptedStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuração da cripto reutilizável do archbase (cifragem de segredos em repouso).
 *
 * <p>Os beans são sempre registrados (o converter JPA é referenciado por {@code @Convert} nas
 * entidades, então precisa existir em qualquer módulo que carregue o mapeamento). A chave vem de
 * {@code archbase.security.crypto.key} (variável de ambiente / secret manager); se ausente, a
 * cifragem só falha quando efetivamente usada. O projeto ganha:
 * <ul>
 *   <li>{@link ArchbaseCryptoService} — AES-GCM (IV aleatório embutido), chave centralizada;</li>
 *   <li>{@link ArchbaseEncryptedStringConverter} — converter JPA ({@code @Convert}) que grava
 *       sempre cifrado com prefixo {@code gcm:} e faz leitura mista (GCM novo + texto puro legado);</li>
 *   <li>{@link ArchbaseColumnReencryptor} — helper para migrações de re-cifragem em massa.</li>
 * </ul>
 *
 * <p>Cada bean usa {@link ConditionalOnMissingBean} para que o projeto possa sobrescrever qualquer
 * um deles (ex.: um converter com leitura legada específica) sem desligar os demais.
 */
@AutoConfiguration
public class ArchbaseCryptoAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ArchbaseCryptoAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ArchbaseCryptoService archbaseCryptoService(
            @Value("${archbase.security.crypto.key:}") String cryptoKey) {
        LOG.info("Archbase crypto auto-configuration ativada (AES-GCM)");
        return new ArchbaseCryptoService(cryptoKey);
    }

    @Bean
    @ConditionalOnMissingBean
    public ArchbaseEncryptedStringConverter archbaseEncryptedStringConverter(
            ArchbaseCryptoService cryptoService) {
        return new ArchbaseEncryptedStringConverter(cryptoService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ArchbaseColumnReencryptor archbaseColumnReencryptor(ArchbaseCryptoService cryptoService) {
        return new ArchbaseColumnReencryptor(cryptoService);
    }
}
