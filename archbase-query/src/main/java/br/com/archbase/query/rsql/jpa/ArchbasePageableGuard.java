package br.com.archbase.query.rsql.jpa;

import br.com.archbase.query.rsql.common.RSQLCommonSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Limita o tamanho de página solicitado pelos endpoints de consulta, evitando que um cliente peça
 * páginas arbitrariamente grandes (vetor de negação de serviço). A política padrão é <em>clamp</em>:
 * o tamanho é reduzido ao máximo permitido e um aviso é registrado — sem quebrar clientes existentes.
 *
 * <p>O limite é configurável globalmente via {@link RSQLCommonSupport#setMaxPageSize(int)} (padrão
 * {@code 1000}). Um limite menor ou igual a zero desativa o guard.
 */
@Slf4j
public final class ArchbasePageableGuard {

    private ArchbasePageableGuard() {
    }

    /**
     * Ajusta um {@link Pageable} usando o limite global de {@link RSQLCommonSupport#getMaxPageSize()}.
     */
    public static Pageable guard(Pageable pageable) {
        return guard(pageable, RSQLCommonSupport.getMaxPageSize());
    }

    /**
     * Ajusta um {@link Pageable} ao {@code maxPageSize} informado, preservando página e ordenação.
     */
    public static Pageable guard(Pageable pageable, int maxPageSize) {
        if (pageable == null || maxPageSize <= 0 || !pageable.isPaged()) {
            return pageable;
        }
        if (pageable.getPageSize() > maxPageSize) {
            log.warn("Tamanho de página solicitado [{}] excede o máximo permitido [{}]; ajustando (clamp).",
                    pageable.getPageSize(), maxPageSize);
            return PageRequest.of(pageable.getPageNumber(), maxPageSize, pageable.getSort());
        }
        return pageable;
    }

    /**
     * Ajusta um valor de tamanho de página ao limite global, retornando o valor permitido.
     */
    public static int clampSize(int size) {
        int max = RSQLCommonSupport.getMaxPageSize();
        if (max > 0 && size > max) {
            log.warn("Tamanho de página solicitado [{}] excede o máximo permitido [{}]; ajustando (clamp).", size, max);
            return max;
        }
        return size;
    }
}
