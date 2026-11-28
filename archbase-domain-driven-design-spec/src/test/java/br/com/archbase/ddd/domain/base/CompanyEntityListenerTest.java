package br.com.archbase.ddd.domain.base;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("CompanyEntityListener")
class CompanyEntityListenerTest {

    private final CompanyEntityListener listener = new CompanyEntityListener();

    @AfterEach
    void limpar() {
        ArchbaseTenantContext.clear();
    }

    private static CompanyPersistenceEntityBase daEmpresa(String empresa) {
        CompanyPersistenceEntityBase e = new CompanyPersistenceEntityBase();
        e.setCompanyId(empresa);
        return e;
    }

    @Test
    @DisplayName("criação: carimba a empresa do contexto")
    void criacao() {
        ArchbaseTenantContext.setCompanyId("empresa-a");
        CompanyPersistenceEntityBase e = daEmpresa(null);
        listener.prePersist(e);
        assertEquals("empresa-a", e.getCompanyId());
    }

    @Test
    @DisplayName("criação sem empresa no contexto mantém a que a entidade trouxe")
    void criacaoSemContexto() {
        CompanyPersistenceEntityBase e = daEmpresa("empresa-explicita");
        listener.prePersist(e);
        assertEquals("empresa-explicita", e.getCompanyId());
    }

    @Test
    @DisplayName("update sem empresa no contexto não apaga a empresa (antes, virava null)")
    void updateSemContexto() {
        CompanyPersistenceEntityBase e = daEmpresa("empresa-a");
        listener.preUpdate(e);
        assertEquals("empresa-a", e.getCompanyId());
    }

    @Test
    @DisplayName("update com outra empresa no contexto não move o registro (antes, movia)")
    void updateComOutraEmpresa() {
        ArchbaseTenantContext.setCompanyId("empresa-b");
        CompanyPersistenceEntityBase e = daEmpresa("empresa-a");
        listener.preUpdate(e);
        assertEquals("empresa-a", e.getCompanyId());
    }

    @Test
    @DisplayName("update de registro sem empresa preenche com a do contexto")
    void updatePreencheVazio() {
        ArchbaseTenantContext.setCompanyId("empresa-b");
        CompanyPersistenceEntityBase e = daEmpresa(null);
        listener.preUpdate(e);
        assertEquals("empresa-b", e.getCompanyId());
    }

    @Test
    @DisplayName("remoção não mexe na empresa")
    void remocao() {
        CompanyPersistenceEntityBase e = daEmpresa("empresa-a");
        listener.preRemove(e);
        assertEquals("empresa-a", e.getCompanyId());
        CompanyPersistenceEntityBase semEmpresa = daEmpresa(null);
        listener.preRemove(semEmpresa);
        assertNull(semEmpresa.getCompanyId());
    }
}
