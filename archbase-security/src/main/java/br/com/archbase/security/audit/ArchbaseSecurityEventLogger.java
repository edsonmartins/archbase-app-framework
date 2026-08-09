package br.com.archbase.security.audit;

import br.com.archbase.security.persistence.SecurityEventEntity;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.repository.SecurityEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registra os acontecimentos de segurança.
 *
 * <p><b>Nunca derruba o que está auditando.</b> Toda falha aqui vira log e nada mais: uma trilha que
 * impede o login de acontecer troca um problema pequeno — perder um registro — por um grande, deixar
 * as pessoas de fora do sistema. Pela mesma razão cada evento é gravado em transação própria
 * ({@code REQUIRES_NEW}): sem isso, um erro ao registrar a negação marcaria para rollback a
 * transação da requisição que estava apenas sendo negada.
 *
 * <p>Fica inteiramente inerte enquanto {@code archbase.security.audit.enabled} for falso, que é o
 * padrão — a mesma chave da trilha de alterações, para não haver dois interruptores de auditoria.
 */
@Service
public class ArchbaseSecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseSecurityEventLogger.class);

    /** Detalhe é uma coluna, não um relatório: o que passar disso é truncado em vez de estourar. */
    private static final int LIMITE_DETALHE = 500;

    private final SecurityEventJpaRepository repository;

    @Value("${archbase.security.audit.enabled:false}")
    private boolean habilitada;

    public ArchbaseSecurityEventLogger(SecurityEventJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginBemSucedido(String usuario) {
        registrar(SecurityEventType.LOGIN, usuario, true, null, null, null);
    }

    /**
     * @param motivo por que não entrou — senha errada, conta desativada, credencial expirada.
     *               Guardado porque distingue erro do usuário de tentativa de invasão.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginFalhou(String usuario, String motivo) {
        registrar(SecurityEventType.LOGIN_FALHOU, usuario, false, null, null, motivo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logout(String usuario, String motivo) {
        registrar(SecurityEventType.LOGOUT, usuario, true, null, null, motivo);
    }

    /**
     * @param portao qual dos cinco portões recusou. É o que torna o registro acionável: saber que
     *               parou em LEVEL manda ajustar nível; em GRANT, manda conceder.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void acessoNegado(String usuario, String recurso, String acao, String portao) {
        registrar(SecurityEventType.ACESSO_NEGADO, usuario, false, recurso, acao, portao);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void simulacao(String quemSimulou, String sujeito, String recurso, String acao) {
        registrar(SecurityEventType.SIMULACAO, quemSimulou, true, recurso, acao,
                sujeito == null ? null : "sujeito: " + sujeito);
    }

    private void registrar(SecurityEventType tipo, String usuario, boolean sucesso,
                             String recurso, String acao, String detalhe) {
        if (!habilitada) {
            return;
        }
        try {
            repository.save(SecurityEventEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .tipo(tipo)
                    .dataHora(LocalDateTime.now())
                    .usuario(usuario)
                    .tenantId(tenantAtual())
                    .origem(origemAtual())
                    .recurso(recurso)
                    .acao(acao)
                    .detalhe(truncar(detalhe))
                    .sucesso(sucesso)
                    .build());
        } catch (Exception e) {
            // Deliberadamente engolido: ver o javadoc da classe.
            log.warn("[segurança] Não foi possível registrar o evento {} de {}: {}",
                    tipo, usuario, e.getMessage());
        }
    }

    private String truncar(String texto) {
        if (texto == null || texto.length() <= LIMITE_DETALHE) {
            return texto;
        }
        return texto.substring(0, LIMITE_DETALHE);
    }

    private String tenantAtual() {
        try {
            return ArchbaseTenantContext.getTenantId();
        } catch (Exception e) {
            return null;
        }
    }

    private String origemAtual() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
                return atributos.getRequest().getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Não foi possível resolver a origem do evento", e);
        }
        return null;
    }
}
