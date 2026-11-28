package br.com.archbase.ddd.domain.base;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * Carimba a empresa do contexto ({@code X-COMPANY-ID}) no registro.
 *
 * <p>A empresa de um registro é a da criação. Até a 3.5.0 este listener gravava a empresa do contexto também no
 * update e na remoção: editar sem empresa no contexto apagava o {@code companyId} do registro, e editar com outra
 * empresa no contexto o movia para ela.
 */
public class CompanyEntityListener {

    /** Na criação, a empresa do contexto; sem ela, mantém a que a entidade já trouxer. */
    @PrePersist
    public void prePersist(CompanyPersistenceEntityBase entity) {
        String doContexto = ArchbaseTenantContext.getCompanyId();
        if (doContexto != null && !doContexto.isBlank()) {
            entity.setCompanyId(doContexto);
        }
    }

    /** Não altera a empresa: só preenche a de um registro que ainda não tem. */
    @PreUpdate
    public void preUpdate(CompanyPersistenceEntityBase entity) {
        String doContexto = ArchbaseTenantContext.getCompanyId();
        if (entity.getCompanyId() == null && doContexto != null && !doContexto.isBlank()) {
            entity.setCompanyId(doContexto);
        }
    }

    /** Remoção não carimba nada. */
    @PreRemove
    public void preRemove(CompanyPersistenceEntityBase entity) {
        // intencionalmente vazio — ver o javadoc da classe
    }
}
