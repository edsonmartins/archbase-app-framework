package br.com.archbase.query.rls.support;

import br.com.archbase.query.rls.support.exceptions.ArchbaseRowLevelSecurityFilterException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;


import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

//@Aspect
//@Component
//@SuppressWarnings({"rawtypes"})
public class ArchbaseQueryMonitor {

//    @Autowired
//    private ApplicationContext context;
//
//    @Autowired
//    private ArchbaseRowLevelSecurityFilterEntityLocator entityLocator;
//
//    @Autowired
//    private EntityManager entityManager;
//
//    @Around("execution(* jakarta.persistence.EntityManager.createQuery(..))")
//    public Object criteriaQueryIntercept(ProceedingJoinPoint joinPoint) throws Throwable {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        Iterator<? extends GrantedAuthority> rolesItr = auth.getAuthorities().iterator();
//
//        for (Object arg : joinPoint.getArgs()) {
//            if (arg instanceof CriteriaQuery) {
//                CriteriaQuery<?> criteriaQuery = (CriteriaQuery<?>) arg;
//                for (Root<?> from : criteriaQuery.getRoots()) {
//                    Class<?> currentEntityClass = from.getJavaType();
//                    applyDynamicFilters(rolesItr, criteriaQuery, from, currentEntityClass);
//                }
//            } else if (arg instanceof String) {
//                String criteriaQuery = (String) arg;
//                applyFilterByEntity(rolesItr, criteriaQuery);
//            }
//        }
//        return joinPoint.proceed();
//    }
//
//    private void applyFilterByEntity(Iterator<? extends GrantedAuthority> rolesItr, String criteriaQuery) throws ArchbaseRowLevelSecurityFilterException {
//        Session session = entityManager.unwrap(Session.class);
//        SessionFactoryImplementor sessionFactory = session.getSessionFactory();
//        QueryTranslatorImpl queryTranslator = new QueryTranslatorImpl("QM" + UUID.randomUUID(), criteriaQuery, Collections.emptyMap(), sessionFactory);
//        queryTranslator.compile(Collections.emptyMap(), true);
//        Statement statement = queryTranslator.getSqlAst();
//        if (statement instanceof QueryNode) {
//            QueryNode node = (QueryNode) statement;
//            FromClause fromClause = node.getFromClause();
//            if (fromClause != null && fromClause.getFromElement() != null) {
//                FromElement fromElement = fromClause.getFromElement();
//                String className = fromElement.getClassName();
//                Set<EntityType<?>> entities = sessionFactory.getMetamodel().getEntities();
//                for (EntityType<?> et : entities) {
//                    if (et.getJavaType().getSimpleName().equals(className)) {
//                        applyDynamicFilters(rolesItr, criteriaQuery, et.getJavaType());
//                    }
//                }
//            }
//        }
//    }
//
//    private void applyDynamicFilters(Iterator<? extends GrantedAuthority> rolesItr, CriteriaQuery<?> criteriaQuery, Root<?> from, Class<?> currentEntityClass
//
//    ) throws ArchbaseRowLevelSecurityFilterException {
//        Class<?>[] filterClasses = entityLocator.get(currentEntityClass);
//
//        if (filterClasses != null) {
//            for (Class<?> filterClass : filterClasses) {
//                RowLevelSecurityFilter filter = context.getBean(filterClass);
//                filter.filter(from, criteriaQuery, rolesItr);
//            }
//        }
//    }
//
//    private void applyDynamicFilters(Iterator<? extends GrantedAuthority> rolesItr, String criteriaQuery, Class<?> currentEntityClass) throws ArchbaseRowLevelSecurityFilterException {
//        Class<?>[] filterClasses = entityLocator.get(currentEntityClass);
//
//        if (filterClasses != null) {
//            for (Class<?> filterClass : filterClasses) {
//                RowLevelSecurityFilter filter = context.getBean(filterClass);
//                filter.filter(criteriaQuery, rolesItr);
//            }
//        }
//    }
}