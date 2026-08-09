package br.com.archbase.security.audit;

import br.com.archbase.security.repository.SecurityEventJpaRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Apaga o que passou do prazo de retenção.
 *
 * <p>Sem purga, a trilha só cresce — e a de eventos cresce rápido, porque registra cada login. O
 * problema não é o disco: é que uma consulta de investigação sobre uma tabela de dezenas de milhões
 * de linhas deixa de ser viável na hora em que mais se precisa dela.
 *
 * <p>O padrão de doze meses cobre o ciclo comum de auditoria — reclamação, investigação, resposta —
 * e é ajustável em {@code archbase.security.audit.retention-months}. Zero desliga a purga, para quem
 * tem exigência regulatória de guardar indefinidamente e trata o crescimento por particionamento.
 */
@Component
@ConditionalOnProperty(name = "archbase.security.audit.enabled", havingValue = "true")
public class ArchbaseAuditRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseAuditRetentionJob.class);

    /**
     * As tabelas de trilha do Envers, na ordem em que precisam ser limpas.
     *
     * <p>A ordem importa: cada linha de {@code _AUD} aponta para uma revisão. Apagar a revisão antes
     * deixaria as linhas órfãs — ou, com a chave estrangeira declarada, faria a purga falhar inteira.
     */
    private static final List<String> TABELAS_AUD = List.of(
            "SEGURANCA_AUD",
            "SEGURANCA_PERMISSAO_AUD",
            "SEGURANCA_ACAO_AUD",
            "SEGURANCA_RECURSO_AUD",
            "SEGURANCA_GRUPO_USUARIO_AUD");

    private final SecurityEventJpaRepository eventRepository;
    private final EntityManager entityManager;

    @Value("${archbase.security.audit.retention-months:12}")
    private int mesesDeRetencao;

    public ArchbaseAuditRetentionJob(SecurityEventJpaRepository eventRepository, EntityManager entityManager) {
        this.eventRepository = eventRepository;
        this.entityManager = entityManager;
    }

    /**
     * Roda de madrugada, uma vez por dia.
     *
     * <p>Horário escolhido pelo motivo óbvio — apagar em lote toma recurso do banco — e frequência
     * diária em vez de mensal porque um lote diário é pequeno, enquanto o acumulado de um mês pode
     * travar a tabela por tempo demais.
     */
    @Scheduled(cron = "${archbase.security.audit.retention-cron:0 30 3 * * *}")
    @Transactional
    public void purgar() {
        if (mesesDeRetencao <= 0) {
            log.debug("[segurança] Purga da trilha desligada (retention-months={}).", mesesDeRetencao);
            return;
        }

        LocalDateTime limite = LocalDateTime.now().minusMonths(mesesDeRetencao);

        int eventos = eventRepository.apagarAnterioresA(limite);

        int revisoes = purgarRevisoes(limite);

        if (eventos > 0 || revisoes > 0) {
            log.info("[segurança] Purga da trilha: {} evento(s) e {} revisão(ões) anteriores a {} removidos.",
                    eventos, revisoes, limite);
        }
    }

    /**
     * Limpa as tabelas de alteração e, só depois, as revisões que ficaram sem nenhuma linha.
     *
     * <p>Em SQL nativo porque as tabelas {@code _AUD} não têm entidade mapeada — são geradas pelo
     * Envers, e não existe repositório para elas.
     */
    private int purgarRevisoes(LocalDateTime limite) {
        try {
            for (String tabela : TABELAS_AUD) {
                entityManager.createNativeQuery(
                                "DELETE FROM " + tabela + " WHERE ID_REVISAO IN "
                                        + "(SELECT ID_REVISAO FROM SEGURANCA_REVISAO WHERE DH_REVISAO < :limite)")
                        .setParameter("limite", limite)
                        .executeUpdate();
            }
            return entityManager.createNativeQuery(
                            "DELETE FROM SEGURANCA_REVISAO WHERE DH_REVISAO < :limite")
                    .setParameter("limite", limite)
                    .executeUpdate();
        } catch (Exception e) {
            // A purga falhar não pode derrubar a aplicação nem a purga dos eventos, que já ocorreu.
            // O aviso é suficiente: na próxima madrugada tenta de novo.
            log.warn("[segurança] Não foi possível purgar as revisões da trilha: {}", e.getMessage());
            return 0;
        }
    }
}
