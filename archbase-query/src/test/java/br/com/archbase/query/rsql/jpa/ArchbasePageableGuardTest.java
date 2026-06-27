package br.com.archbase.query.rsql.jpa;

import br.com.archbase.query.rsql.common.RSQLCommonSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Testa o guard de tamanho de página: clamp (não-quebra) ao máximo configurado, com preservação de
 * página e ordenação, e desativação quando o limite é menor ou igual a zero.
 */
class ArchbasePageableGuardTest {

    @BeforeEach
    void setUp() {
        RSQLCommonSupport.setMaxPageSize(100);
    }

    @AfterEach
    void tearDown() {
        RSQLCommonSupport.setMaxPageSize(1000); // restaura o padrão
    }

    @Test
    void clampSizeReduzAcimaDoMaximo() {
        assertEquals(100, ArchbasePageableGuard.clampSize(500));
    }

    @Test
    void clampSizeMantemAbaixoDoMaximo() {
        assertEquals(50, ArchbasePageableGuard.clampSize(50));
    }

    @Test
    void clampSizeDesativadoQuandoMaximoZeroOuNegativo() {
        RSQLCommonSupport.setMaxPageSize(0);
        assertEquals(99999, ArchbasePageableGuard.clampSize(99999));
    }

    @Test
    void guardClampaPreservandoPaginaEOrdenacao() {
        Sort sort = Sort.by("name").ascending();
        Pageable result = ArchbasePageableGuard.guard(PageRequest.of(3, 500, sort));

        assertEquals(100, result.getPageSize());
        assertEquals(3, result.getPageNumber());
        assertEquals(sort, result.getSort());
    }

    @Test
    void guardNaoAlteraPageableDentroDoLimite() {
        Pageable original = PageRequest.of(0, 50);
        assertSame(original, ArchbasePageableGuard.guard(original));
    }

    @Test
    void guardNaoAlteraUnpaged() {
        Pageable unpaged = Pageable.unpaged();
        assertSame(unpaged, ArchbasePageableGuard.guard(unpaged));
    }
}
