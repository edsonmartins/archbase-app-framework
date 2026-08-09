package br.com.archbase.security.audit;

import br.com.archbase.security.persistence.ArchbaseSecurityRevision;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.hibernate.envers.RevisionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Preenche o "quem" e o "de onde" de cada revisão.
 *
 * <p>Não é um bean do Spring: o Envers instancia o listener por conta própria, então tudo aqui é
 * lido de contextos estáticos — o de segurança, o de tenant e o da requisição. É a razão de este
 * listener não injetar nada.
 *
 * <p><b>Nunca deixa a gravação falhar.</b> Uma auditoria que impede a operação de acontecer é pior
 * que uma auditoria incompleta: o que estava sendo salvo é a mudança real de permissão, e derrubá-la
 * porque não se descobriu o nome do usuário troca um problema pequeno por um grande. Toda falha aqui
 * vira log e campo nulo.
 */
public class ArchbaseSecurityRevisionListener implements RevisionListener {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseSecurityRevisionListener.class);

    @Override
    public void newRevision(Object revisionEntity) {
        ArchbaseSecurityRevision revisao = (ArchbaseSecurityRevision) revisionEntity;
        revisao.setDataHora(LocalDateTime.now());
        revisao.setUsuario(usuarioAtual());
        revisao.setTenantId(tenantAtual());
        revisao.setOrigem(origemAtual());
    }

    /**
     * Quem está autenticado, pelo mesmo critério do resto do framework.
     *
     * <p>Fica nulo de propósito quando não há autenticação: seed, carga e job alteram segurança sem
     * usuário, e inventar um nome ali seria pior que admitir a ausência.
     */
    private String usuarioAtual() {
        try {
            Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
            if (autenticacao == null || !autenticacao.isAuthenticated()
                    || autenticacao instanceof AnonymousAuthenticationToken) {
                return null;
            }
            return autenticacao.getName();
        } catch (Exception e) {
            log.debug("Não foi possível resolver o usuário da revisão", e);
            return null;
        }
    }

    private String tenantAtual() {
        try {
            return ArchbaseTenantContext.getTenantId();
        } catch (Exception e) {
            log.debug("Não foi possível resolver o tenant da revisão", e);
            return null;
        }
    }

    /**
     * O endereço de origem, quando existe requisição.
     *
     * <p>Lê o cabeçalho encaminhado apenas se {@code archbase.security.client-ip.trust-forwarded-for}
     * estiver ligado — a mesma regra do rate limit. Sem proxy que sobrescreva o cabeçalho, ele é
     * texto que o cliente escolhe, e registrar isso como fato seria pior que não registrar.
     */
    private String origemAtual() {
        try {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos)) {
                return null;
            }
            return atributos.getRequest().getRemoteAddr();
        } catch (Exception e) {
            log.debug("Não foi possível resolver a origem da revisão", e);
            return null;
        }
    }
}
