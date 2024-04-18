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
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ArchbaseTenantServiceAspect {

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
        if (StringUtils.isNotEmpty(ArchbaseTenantContext.getCompanyId())) {
            final Session session = this.entityManager.unwrap(Session.class);
            final Filter filter =
                    session
                            .enableFilter("companyFilter")
                            .setParameter(
                                    "companyId", ArchbaseTenantContext.getCompanyId());
            filter.validate();
        }

        return pjp.proceed();
    }
}