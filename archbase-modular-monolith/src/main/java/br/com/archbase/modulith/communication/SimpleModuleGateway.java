package br.com.archbase.modulith.communication;

import br.com.archbase.modulith.communication.contracts.ModuleRequest;
import br.com.archbase.modulith.communication.contracts.ModuleRequestHandler;
import br.com.archbase.modulith.core.ModuleRegistry;
import br.com.archbase.modulith.core.ModuleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Implementação simples do ModuleGateway usando chamadas diretas in-process.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public class SimpleModuleGateway implements ModuleGateway {

    private static final Logger log = LoggerFactory.getLogger(SimpleModuleGateway.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final ModuleRegistry moduleRegistry;
    private final Map<String, Map<Class<?>, ModuleRequestHandler<?, ?>>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public SimpleModuleGateway(ModuleRegistry moduleRegistry) {
        this(moduleRegistry, Executors.newCachedThreadPool());
    }

    public SimpleModuleGateway(ModuleRegistry moduleRegistry, ExecutorService executor) {
        this.moduleRegistry = moduleRegistry;
        this.executor = executor;
    }

    /**
     * Registra um handler para um tipo de requisição em um módulo.
     *
     * @param moduleName  Nome do módulo
     * @param requestType Tipo da requisição
     * @param handler     Handler que processará a requisição
     * @param <T>         Tipo da requisição
     * @param <R>         Tipo do resultado
     */
    public <T extends ModuleRequest<R>, R> void registerHandler(
            String moduleName,
            Class<T> requestType,
            ModuleRequestHandler<T, R> handler) {

        handlers.computeIfAbsent(moduleName, k -> new ConcurrentHashMap<>())
                .put(requestType, handler);

        log.debug("Registered handler for {} in module {}", requestType.getSimpleName(), moduleName);
    }

    /**
     * Remove um handler registrado.
     *
     * @param moduleName  Nome do módulo
     * @param requestType Tipo da requisição
     */
    public void unregisterHandler(String moduleName, Class<?> requestType) {
        Map<Class<?>, ModuleRequestHandler<?, ?>> moduleHandlers = handlers.get(moduleName);
        if (moduleHandlers != null) {
            moduleHandlers.remove(requestType);
        }
    }

    @Override
    public <R> R execute(String targetModule, ModuleRequest<R> request) {
        return execute(targetModule, request, DEFAULT_TIMEOUT);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R execute(String targetModule, ModuleRequest<R> request, Duration timeout) {
        log.debug("Executing request {} on module {}", request.getOperationName(), targetModule);

        // Verificar se módulo existe e está disponível
        if (!isModuleAvailable(targetModule)) {
            throw new ModuleNotFoundException(targetModule,
                    "Module '" + targetModule + "' is not available");
        }

        // Obter handler
        Map<Class<?>, ModuleRequestHandler<?, ?>> moduleHandlers = handlers.get(targetModule);
        if (moduleHandlers == null) {
            throw new ModuleRequestException(targetModule, request.getOperationName(),
                    "No handlers registered for module: " + targetModule);
        }

        ModuleRequestHandler<ModuleRequest<R>, R> handler =
                (ModuleRequestHandler<ModuleRequest<R>, R>) moduleHandlers.get(request.getClass());

        if (handler == null) {
            throw new ModuleRequestException(targetModule, request.getOperationName(),
                    "No handler found for request type: " + request.getClass().getSimpleName());
        }

        // Executar com timeout
        try {
            CompletableFuture<R> future = CompletableFuture.supplyAsync(
                    () -> handler.handle(request), executor);

            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            throw new ModuleRequestTimeoutException(targetModule, request.getOperationName(), timeout);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new ModuleRequestException(targetModule, request.getOperationName(),
                    "Error executing request: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModuleRequestException(targetModule, request.getOperationName(),
                    "Request interrupted", e);
        }
    }

    @Override
    public <R> CompletableFuture<R> executeAsync(String targetModule, ModuleRequest<R> request) {
        return CompletableFuture.supplyAsync(() -> execute(targetModule, request), executor);
    }

    @Override
    public boolean isModuleAvailable(String moduleName) {
        return moduleRegistry.getModule(moduleName)
                .map(m -> m.getState() == ModuleState.STARTED && m.isEnabled())
                .orElse(false);
    }

    @Override
    public boolean waitForModule(String moduleName, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        long pollInterval = 100; // ms

        while (System.currentTimeMillis() < deadline) {
            if (isModuleAvailable(moduleName)) {
                return true;
            }
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * Encerra o executor de forma limpa.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
