package br.com.archbase.multitenancy.interceptor;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Liga o {@code companyFilter} do Hibernate em toda chamada de repositório quando há empresa no
 * {@link ArchbaseTenantContext}.
 *
 * <p>O filtro só é declarado em {@code CompanyPersistenceEntityBase}. Numa aplicação sem entidade
 * que estenda essa base ele não existe na {@code SessionFactory}, e {@code enableFilter} lançava
 * {@code UnknownFilterException} — dentro do filtro JWT, na busca do usuário, o que devolvia 401 a
 * toda requisição com {@code X-COMPANY-ID}. Agora, sem o filtro, a empresa do contexto é ignorada
 * para filtragem e a chamada segue.
 *
 * <p>O filtro não é desligado ao fim da chamada, de propósito. Ele vive o tempo da sessão e o
 * parâmetro é reescrito a cada chamada, então uma troca de empresa é respeitada. Desligá-lo no
 * {@code finally} faria as consultas feitas na mesma sessão depois do retorno do repositório (JPQL
 * direto pelo {@code EntityManager}, por exemplo) saírem sem filtro — abriria dados que hoje não
 * abrem. Se o contexto perder a empresa no meio da sessão, o filtro continua com a anterior: mais
 * restritivo, nunca mais permissivo.
 */
@Aspect
@Component
public class ArchbaseTenantServiceAspect {

    static final String COMPANY_FILTER = "companyFilter";

    private static final Logger log = LoggerFactory.getLogger(ArchbaseTenantServiceAspect.class);

    @PersistenceContext
    public EntityManager entityManager;


    @Pointcut("execution(public * org.springframework.data.repository.Repository+.*(..))")
    void isRepository() {
        /* aspect */
    }

    @Pointcut(value = "isRepository()")
    void enableMultiTenancy() {
        /* aspect */
    }

    @Around("execution(public * *(..)) && enableMultiTenancy()")
    public Object aroundExecution(final ProceedingJoinPoint pjp) throws Throwable {
        final String companyId = ArchbaseTenantContext.getCompanyId();
        if (StringUtils.isNotEmpty(companyId)) {
            final Session session = this.entityManager.unwrap(Session.class);
            if (session.getSessionFactory().getDefinedFilterNames().contains(COMPANY_FILTER)) {
                final Filter filter = session.enableFilter(COMPANY_FILTER).setParameter("companyId", companyId);
                filter.validate();
            } else if (log.isDebugEnabled()) {
                // debug, não warn: é o caminho de toda chamada de repositório.
                log.debug("Filtro '{}' não definido no persistence unit (nenhuma entidade estende "
                        + "CompanyPersistenceEntityBase); empresa '{}' do contexto ignorada para filtragem em {}",
                        COMPANY_FILTER, companyId, pjp.getSignature().toShortString());
            }
        }

        return pjp.proceed();
    }
}
