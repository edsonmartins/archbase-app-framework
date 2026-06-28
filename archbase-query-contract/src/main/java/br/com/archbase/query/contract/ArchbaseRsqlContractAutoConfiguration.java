package br.com.archbase.query.contract;

import io.github.ggomarighetti.rsqljpasearch.rsql.backend.RsqlBackendAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Registra o {@link ArchbaseRsqlBackendAdapter} como backend de RSQL da biblioteca de contrato.
 *
 * <p>É processada <em>antes</em> da auto-configuração de engine da {@code rsql-jpa-search} (cujo bean
 * de backend é {@code @ConditionalOnMissingBean}), de modo que o adapter do Archbase passa a ser o
 * backend efetivo, substituindo o padrão (perplexhub), sem que o usuário precise excluir nada.
 *
 * <p>Pode ser desligada com {@code archbase.query.contract.enabled=false}, e o usuário ainda pode
 * fornecer o próprio bean {@link RsqlBackendAdapter} para sobrepor este.
 */
@AutoConfiguration(beforeName = "io.github.ggomarighetti.rsqljpasearch.autoconfigure.RsqlJpaSearchEngineAutoConfiguration")
@ConditionalOnClass(RsqlBackendAdapter.class)
@ConditionalOnProperty(prefix = "archbase.query.contract", name = "enabled", matchIfMissing = true)
public class ArchbaseRsqlContractAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RsqlBackendAdapter archbaseRsqlBackendAdapter() {
        return new ArchbaseRsqlBackendAdapter();
    }
}
