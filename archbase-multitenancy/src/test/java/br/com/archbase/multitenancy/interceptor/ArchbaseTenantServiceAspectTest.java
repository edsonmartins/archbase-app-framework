package br.com.archbase.multitenancy.interceptor;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.ddd.domain.base.CompanyPersistenceEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Roda o aspecto contra uma {@code SessionFactory} real (H2), com e sem entidade que declare o
 * {@code companyFilter}. O proceed do join point é a consulta que o repositório faria.
 */
@DisplayName("ArchbaseTenantServiceAspect")
class ArchbaseTenantServiceAspectTest {

    private static final String TENANT = "tenant-a";

    @AfterEach
    void tearDown() {
        ArchbaseTenantContext.clear();
    }

    @Nested
    @DisplayName("persistence unit sem CompanyPersistenceEntityBase")
    class SemFiltroDeEmpresa {

        @Test
        @DisplayName("empresa no contexto: a chamada segue, sem UnknownFilterException")
        void empresaNoContextoNaoQuebra() throws Throwable {
            try (SessionFactory sf = sessionFactory(PlainEntity.class); Session session = sf.openSession()) {
                assertThat(sf.getDefinedFilterNames()).doesNotContain(ArchbaseTenantServiceAspect.COMPANY_FILTER);
                session.getTransaction().begin();
                session.persist(new PlainEntity("p1"));
                session.flush();

                ArchbaseTenantContext.setCompanyId("company-1");
                List<?> result = (List<?>) aspect(session).aroundExecution(joinPoint(
                        () -> session.createQuery("from PlainEntity", PlainEntity.class).getResultList()));

                assertThat(result).hasSize(1);
                assertThat(session.getEnabledFilter(ArchbaseTenantServiceAspect.COMPANY_FILTER)).isNull();
                session.getTransaction().rollback();
            }
        }

        @Test
        @DisplayName("sem empresa no contexto: nada muda")
        void semEmpresaNadaMuda() throws Throwable {
            try (SessionFactory sf = sessionFactory(PlainEntity.class); Session session = sf.openSession()) {
                session.getTransaction().begin();
                session.persist(new PlainEntity("p1"));
                session.flush();

                List<?> result = (List<?>) aspect(session).aroundExecution(joinPoint(
                        () -> session.createQuery("from PlainEntity", PlainEntity.class).getResultList()));

                assertThat(result).hasSize(1);
                session.getTransaction().rollback();
            }
        }
    }

    @Nested
    @DisplayName("persistence unit com CompanyPersistenceEntityBase")
    class ComFiltroDeEmpresa {

        @Test
        @DisplayName("empresa no contexto: linhas de outra empresa não voltam")
        void filtroContinuaAplicado() throws Throwable {
            try (SessionFactory sf = sessionFactory(CompanyEntity.class); Session session = sf.openSession()) {
                popular(session);

                ArchbaseTenantContext.setCompanyId("company-1");
                List<?> result = (List<?>) aspect(session).aroundExecution(joinPoint(
                        () -> session.createQuery("from CompanyEntity", CompanyEntity.class).getResultList()));

                assertThat(result).extracting(e -> ((CompanyEntity) e).getCompanyId()).containsExactly("company-1");
                assertThat(session.getEnabledFilter(ArchbaseTenantServiceAspect.COMPANY_FILTER)).isNotNull();
                session.getTransaction().rollback();
            }
        }

        @Test
        @DisplayName("troca de empresa na mesma sessão: o parâmetro é reescrito")
        void trocaDeEmpresa() throws Throwable {
            try (SessionFactory sf = sessionFactory(CompanyEntity.class); Session session = sf.openSession()) {
                popular(session);
                ArchbaseTenantServiceAspect aspect = aspect(session);
                ProceedingJoinPoint query = joinPoint(
                        () -> session.createQuery("from CompanyEntity", CompanyEntity.class).getResultList());

                ArchbaseTenantContext.setCompanyId("company-1");
                aspect.aroundExecution(query);
                ArchbaseTenantContext.setCompanyId("company-2");
                List<?> result = (List<?>) aspect.aroundExecution(query);

                assertThat(result).extracting(e -> ((CompanyEntity) e).getCompanyId()).containsExactly("company-2");
                session.getTransaction().rollback();
            }
        }

        @Test
        @DisplayName("sem empresa no contexto: nada muda, o filtro não é ligado")
        void semEmpresaNadaMuda() throws Throwable {
            try (SessionFactory sf = sessionFactory(CompanyEntity.class); Session session = sf.openSession()) {
                popular(session);

                List<?> result = (List<?>) aspect(session).aroundExecution(joinPoint(
                        () -> session.createQuery("from CompanyEntity", CompanyEntity.class).getResultList()));

                assertThat(result).hasSize(2);
                assertThat(session.getEnabledFilter(ArchbaseTenantServiceAspect.COMPANY_FILTER)).isNull();
                session.getTransaction().rollback();
            }
        }

        private void popular(Session session) {
            session.getTransaction().begin();
            ArchbaseTenantContext.setCompanyId("company-1");
            session.persist(new CompanyEntity("c1"));
            ArchbaseTenantContext.setCompanyId("company-2");
            session.persist(new CompanyEntity("c2"));
            session.flush();
            session.clear();
            ArchbaseTenantContext.clear();
        }
    }

    private static ArchbaseTenantServiceAspect aspect(Session session) {
        ArchbaseTenantServiceAspect aspect = new ArchbaseTenantServiceAspect();
        aspect.entityManager = session;
        return aspect;
    }

    private static ProceedingJoinPoint joinPoint(Callable<Object> call) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("TestRepository.findAll()");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenAnswer(inv -> call.call());
        return pjp;
    }

    private static SessionFactory sessionFactory(Class<?> entity) {
        Configuration cfg = new Configuration()
                .addAnnotatedClass(entity)
                .setProperty("hibernate.connection.url",
                        "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("jakarta.persistence.validation.mode", "none");
        cfg.setCurrentTenantIdentifierResolver(new FixedTenantResolver());
        return cfg.buildSessionFactory();
    }

    static class FixedTenantResolver implements CurrentTenantIdentifierResolver<Object> {
        @Override
        public Object resolveCurrentTenantIdentifier() {
            return TENANT;
        }

        @Override
        public boolean validateExistingCurrentSessions() {
            return false;
        }
    }

    @Entity(name = "PlainEntity")
    @Table(name = "PLAIN_ENTITY")
    static class PlainEntity {
        @Id
        private String id;

        protected PlainEntity() {
        }

        PlainEntity(String id) {
            this.id = id;
        }
    }

    @Entity(name = "CompanyEntity")
    @Table(name = "COMPANY_ENTITY")
    static class CompanyEntity extends CompanyPersistenceEntityBase {
        protected CompanyEntity() {
        }

        CompanyEntity(String code) {
            super(code);
        }
    }
}
