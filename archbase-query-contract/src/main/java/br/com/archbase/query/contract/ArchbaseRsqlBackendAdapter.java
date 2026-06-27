package br.com.archbase.query.contract;

import br.com.archbase.query.rsql.jpa.ArchbaseRSQLJPASupport;
import io.github.ggomarighetti.rsqljpasearch.rsql.RsqlCompilationRequest;
import io.github.ggomarighetti.rsqljpasearch.rsql.backend.RsqlBackendAdapter;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

/**
 * Backend de RSQL ({@link RsqlBackendAdapter}) que delega a compilação ao motor do Archbase.
 *
 * <p>A biblioteca {@code rsql-jpa-search} (io.github.ggomarighetti) fornece a camada de
 * contrato/governança — declaração de campos expostos, mapeamento selector→path, allowlist de
 * operadores, validação de valores e paging — e abstrai o motor de tradução via este SPI. Este
 * adapter conecta essa governança ao {@link ArchbaseRSQLJPASupport}, de modo que a
 * {@link Specification} final é produzida pelo motor do Archbase (mantendo as proteções já
 * existentes nele) em vez do backend padrão (perplexhub).
 *
 * <p>Como a requisição validada ({@link RsqlCompilationRequest}) carrega a string RSQL original e a
 * definição, a ponte é direta: usa-se o mapa canônico selector público → caminho JPA exposto por
 * {@code SearchDefinition.filteringPaths()} como {@code propertyPathMapper} e delega-se a
 * {@link ArchbaseRSQLJPASupport#toSpecification(String, boolean, Map)}.
 *
 * <p><strong>Limitação:</strong> a string RSQL é reanalisada pelo parser do Archbase, então os
 * operadores usados na definição devem pertencer ao conjunto suportado pelo Archbase
 * ({@code br.com.archbase.query.rsql.common.RSQLOperators}). Operadores customizados registrados
 * apenas na biblioteca de contrato não são reconhecidos por este backend.
 */
public final class ArchbaseRsqlBackendAdapter implements RsqlBackendAdapter {

    @Override
    public <T> Specification<T> compile(RsqlCompilationRequest<T> request) {
        Map<String, String> propertyPathMapper = request.definition().filteringPaths();
        return ArchbaseRSQLJPASupport.toSpecification(request.rsql(), request.distinct(), propertyPathMapper);
    }
}
