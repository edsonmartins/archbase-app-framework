package br.com.archbase.security.service;

import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Serviço para limpeza e manutenção dos tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "archbase.security.token.cleanup.enabled", havingValue = "true")
public class ArchbaseTokenCleanupService {

    private final AccessTokenJpaRepository tokenRepository;
    
    @Value("${archbase.security.token.cleanup.retention-days:30}")
    private int tokenRetentionDays;

    /**
     * Tarefa agendada para limpar tokens expirados
     * Executada uma vez por dia
     */
    @Scheduled(cron = "${archbase.security.token.cleanup.cron:0 0 1 * * ?}") // Default: 1AM todos os dias
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Iniciando limpeza agendada de tokens expirados");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        
        try {
            // Marcar tokens expirados que ainda não foram marcados como tal
            List<AccessTokenEntity> expiredTokens = tokenRepository.findByExpirationDateBeforeAndExpiredFalse(now);
            
            if (!expiredTokens.isEmpty()) {
                log.info("Marcando {} tokens como expirados", expiredTokens.size());
                expiredTokens.forEach(token -> {
                    token.setExpired(true);
                    token.setRevoked(true);
                });
                tokenRepository.saveAll(expiredTokens);
            } else {
                log.debug("Nenhum token expirado não marcado encontrado");
            }
            
            // Remover tokens muito antigos para limpeza do banco
            LocalDateTime oldestRetentionDate = now.minus(tokenRetentionDays, ChronoUnit.DAYS);
            List<AccessTokenEntity> veryOldTokens = tokenRepository.findExpiredTokensOlderThan(oldestRetentionDate);
            
            if (!veryOldTokens.isEmpty()) {
                log.info("Excluindo {} tokens muito antigos (> {} dias)", veryOldTokens.size(), tokenRetentionDays);
                tokenRepository.deleteAll(veryOldTokens);
            } else {
                log.debug("Nenhum token muito antigo encontrado para exclusão");
            }
            
            log.info("Limpeza de tokens concluída com sucesso");
        } catch (Exception e) {
            log.error("Erro durante limpeza de tokens", e);
        }
    }
    
    /**
     * Método para forçar a limpeza de tokens imediatamente
     * Útil para chamadas administrativas
     */
    @Transactional
    public void forceTokenCleanup() {
        log.info("Iniciando limpeza forçada de tokens");
        cleanupExpiredTokens();
    }
}