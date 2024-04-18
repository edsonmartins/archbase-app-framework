package br.com.archbase.multitenancy.async;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;


public class ArchbaseTenantAwareTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        String tenantId = ArchbaseTenantContext.getTenantId();
        return () -> {
            try {
                ArchbaseTenantContext.setTenantId(tenantId);
                runnable.run();
            } finally {
                ArchbaseTenantContext.setTenantId(null);
            }
        };
    }
}