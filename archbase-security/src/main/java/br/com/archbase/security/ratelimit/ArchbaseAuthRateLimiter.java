package br.com.archbase.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite de tentativas para os fluxos de credencial (login, verificação de MFA, reset de senha).
 *
 * <p><b>Por que existe.</b> Não havia limite nenhum: {@code /auth/authenticate} aceitava tentativas
 * ilimitadas, e o token de reset de senha tem 8 dígitos numéricos — 10^8 combinações, alcançáveis
 * por força bruta online em tempo hábil se ninguém contar as tentativas. Senha forte não protege
 * contra isso; contar tentativas protege.
 *
 * <p><b>Escopo: em memória, por instância.</b> É deliberado — é a proteção que funciona sem exigir
 * Redis nem mudança de infraestrutura de quem atualiza o framework. Num cluster de N instâncias o
 * limite efetivo é N vezes o configurado, o que ainda reduz a força bruta em ordens de grandeza. Se
 * a aplicação precisa de contagem exata e distribuída, declare um bean próprio desta classe
 * apoiado em armazenamento compartilhado.
 */
@Component
@Slf4j
public class ArchbaseAuthRateLimiter {

    /**
     * Desliga a contagem. Existe para ambientes de teste de carga e para quem já resolve isso na
     * borda (WAF, API gateway) — não para uso normal.
     */
    @Value("${archbase.security.rate-limit.enabled:true}")
    private boolean enabled;

    /**
     * Tentativas falhas toleradas dentro da janela, por chave.
     *
     * <p>O padrão é folgado de propósito: 10 erros de senha em 15 minutos não acontece com usuário
     * legítimo, e um limite apertado transformaria a correção em suporte — usuário trancado fora da
     * conta por errar a senha três vezes.
     */
    @Value("${archbase.security.rate-limit.max-attempts:10}")
    private int maxAttempts;

    /** Janela de contagem, em segundos. Passado esse tempo sem bloqueio, a contagem zera. */
    @Value("${archbase.security.rate-limit.window-seconds:900}")
    private long windowSeconds;

    /** Duração do bloqueio depois de estourar o limite, em segundos. */
    @Value("${archbase.security.rate-limit.block-seconds:900}")
    private long blockSeconds;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * @return {@code true} se a chave está bloqueada e a operação deve ser recusada sem sequer
     *         verificar a credencial
     */
    public boolean isBlocked(String key) {
        if (!enabled || key == null) {
            return false;
        }
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.blockedUntil != null && Instant.now().isBefore(attempt.blockedUntil)) {
            return true;
        }
        if (attempt.blockedUntil != null) {
            // Bloqueio venceu: limpa para o usuário legítimo recomeçar do zero.
            attempts.remove(key);
        }
        return false;
    }

    /** Registra uma tentativa falha e bloqueia a chave se o limite for atingido. */
    public void recordFailure(String key) {
        if (!enabled || key == null) {
            return;
        }
        purgeExpired();

        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        synchronized (attempt) {
            Instant now = Instant.now();
            if (attempt.windowStart.plusSeconds(windowSeconds).isBefore(now)) {
                attempt.windowStart = now;
                attempt.count.set(0);
            }
            int current = attempt.count.incrementAndGet();
            if (current >= maxAttempts) {
                attempt.blockedUntil = now.plusSeconds(blockSeconds);
                log.warn("Limite de tentativas atingido para '{}': bloqueado por {}s",
                        key, blockSeconds);
            }
        }
    }

    /** Zera a contagem — chamado quando a credencial confere. */
    public void recordSuccess(String key) {
        if (key != null) {
            attempts.remove(key);
        }
    }

    /** Segundos restantes de bloqueio, para informar o cliente no {@code Retry-After}. */
    public long secondsUntilUnblock(String key) {
        Attempt attempt = key != null ? attempts.get(key) : null;
        if (attempt == null || attempt.blockedUntil == null) {
            return 0;
        }
        long remaining = Duration.between(Instant.now(), attempt.blockedUntil).getSeconds();
        return Math.max(remaining, 0);
    }

    /**
     * Remove entradas vencidas. Sem isto o mapa cresceria sem limite sob um ataque que varia o
     * e-mail a cada tentativa — a própria proteção viraria o vetor de exaustão de memória.
     */
    private void purgeExpired() {
        if (attempts.size() < 10_000) {
            return;
        }
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            Attempt value = entry.getValue();
            boolean blockExpired = value.blockedUntil == null || now.isAfter(value.blockedUntil);
            boolean windowExpired = value.windowStart.plusSeconds(windowSeconds).isBefore(now);
            return blockExpired && windowExpired;
        });
    }

    private static final class Attempt {
        private Instant windowStart = Instant.now();
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant blockedUntil;
    }
}
