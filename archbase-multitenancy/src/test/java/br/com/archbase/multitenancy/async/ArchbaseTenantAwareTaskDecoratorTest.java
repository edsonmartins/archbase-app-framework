package br.com.archbase.multitenancy.async;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ArchbaseTenantAwareTaskDecorator")
class ArchbaseTenantAwareTaskDecoratorTest {

    @AfterEach
    void tearDown() {
        ArchbaseTenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("propaga tenant e company para a thread da tarefa")
    void propagaTenantECompany() {
        ArchbaseTenantContext.setTenantId("tenant-a");
        ArchbaseTenantContext.setCompanyId("company-1");

        AtomicReference<String> tenantVisto = new AtomicReference<>();
        AtomicReference<String> companyVista = new AtomicReference<>();

        Runnable decorada = new ArchbaseTenantAwareTaskDecorator().decorate(() -> {
            tenantVisto.set(ArchbaseTenantContext.getTenantId());
            companyVista.set(ArchbaseTenantContext.getCompanyId());
        });

        // Executa noutra thread: é o cenário real do pool assíncrono.
        runInNewThread(decorada);

        assertThat(tenantVisto.get()).isEqualTo("tenant-a");
        // O company não era propagado: a tarefa rodava sem ele e o companyFilter não era ativado.
        assertThat(companyVista.get()).isEqualTo("company-1");
    }

    @Test
    @DisplayName("propaga o usuário autenticado")
    void propagaSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana@vendax.com.br", null));

        AtomicReference<String> usuarioVisto = new AtomicReference<>();
        Runnable decorada = new ArchbaseTenantAwareTaskDecorator().decorate(() ->
                usuarioVisto.set(SecurityContextHolder.getContext().getAuthentication() != null
                        ? SecurityContextHolder.getContext().getAuthentication().getName()
                        : null));

        runInNewThread(decorada);

        assertThat(usuarioVisto.get()).isEqualTo("ana@vendax.com.br");
    }

    @Test
    @DisplayName("desligada a propagação, o SecurityContext não vai junto")
    void naoPropagaQuandoDesligado() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana@vendax.com.br", null));

        AtomicReference<Object> visto = new AtomicReference<>();
        Runnable decorada = new ArchbaseTenantAwareTaskDecorator(false).decorate(() ->
                visto.set(SecurityContextHolder.getContext().getAuthentication()));

        runInNewThread(decorada);

        assertThat(visto.get()).isNull();
    }

    @Test
    @DisplayName("restaura o contexto anterior da thread — ela vem de um pool")
    void restauraContextoAnterior() {
        ArchbaseTenantContext.setTenantId("tenant-submissor");

        Runnable decorada = new ArchbaseTenantAwareTaskDecorator().decorate(() -> { });

        AtomicReference<String> tenantDepois = new AtomicReference<>();
        runInNewThread(() -> {
            // A thread do pool já estava carregando outro trabalho.
            ArchbaseTenantContext.setTenantId("tenant-preexistente");
            decorada.run();
            tenantDepois.set(ArchbaseTenantContext.getTenantId());
        });

        assertThat(tenantDepois.get()).isEqualTo("tenant-preexistente");
    }

    @Test
    @DisplayName("restaura o contexto mesmo quando a tarefa lança")
    void restauraAposExcecao() {
        ArchbaseTenantContext.setTenantId("tenant-a");

        Runnable decorada = new ArchbaseTenantAwareTaskDecorator().decorate(() -> {
            throw new IllegalStateException("falha na tarefa");
        });

        AtomicReference<String> tenantDepois = new AtomicReference<>("nao-executou");
        runInNewThread(() -> {
            ArchbaseTenantContext.setTenantId("tenant-preexistente");
            assertThatThrownBy(decorada::run).isInstanceOf(IllegalStateException.class);
            tenantDepois.set(ArchbaseTenantContext.getTenantId());
        });

        assertThat(tenantDepois.get()).isEqualTo("tenant-preexistente");
    }

    @Test
    @DisplayName("thread nova herda o tenant do submissor — é InheritableThreadLocal")
    void contextoEhHerdadoPorThreadFilha() {
        // Documenta o porquê de a limpeza importar tanto: qualquer thread criada durante a
        // requisição nasce com o tenant dela, e um ThreadLocal não limpo se propaga adiante.
        ArchbaseTenantContext.setTenantId("tenant-a");

        AtomicReference<String> herdado = new AtomicReference<>();
        runInNewThread(() -> herdado.set(ArchbaseTenantContext.getTenantId()));

        assertThat(herdado.get()).isEqualTo("tenant-a");
    }

    private void runInNewThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
