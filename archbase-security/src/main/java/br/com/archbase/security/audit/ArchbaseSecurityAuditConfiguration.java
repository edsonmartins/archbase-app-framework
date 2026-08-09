package br.com.archbase.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Liga ou desliga a trilha de auditoria do módulo de segurança.
 *
 * <p><b>Por que a chave existe.</b> {@code @Audited} é anotação, e anotação não tem interruptor: uma
 * vez nas entidades, o Envers passa a gravar em toda aplicação que usar o framework. Quem controla o
 * schema por migrations não teria as tabelas {@code _AUD}, e a primeira alteração de permissão
 * falharia — atualizar o framework quebraria a aplicação sem que ninguém tivesse pedido auditoria.
 *
 * <p>Desligar os listeners do Envers deixa as anotações inertes: o mapeamento continua lá, nada é
 * gravado, e nenhuma tabela é exigida. É o que este bean faz enquanto
 * {@code archbase.security.audit.enabled} for {@code false}, o padrão.
 *
 * <p>Para ligar são dois passos, nesta ordem: criar as tabelas (ver
 * {@code deployment/sql/auditoria-seguranca-*.sql}) e então virar a chave. Invertê-los derruba a
 * aplicação na primeira escrita.
 */
@Configuration
public class ArchbaseSecurityAuditConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseSecurityAuditConfiguration.class);

    @Value("${archbase.security.audit.enabled:false}")
    private boolean habilitada;

    @Bean
    public HibernatePropertiesCustomizer archbaseAuditHibernateCustomizer() {
        return (Map<String, Object> propriedades) -> {
            if (habilitada) {
                // O sufixo e o nome da coluna de tipo ficam explícitos: são contrato com o script de
                // criação das tabelas, e o padrão do Envers já mudou entre versões.
                propriedades.putIfAbsent("org.hibernate.envers.audit_table_suffix", "_AUD");
                propriedades.putIfAbsent("org.hibernate.envers.revision_field_name", "ID_REVISAO");
                propriedades.putIfAbsent("org.hibernate.envers.revision_type_field_name", "TP_REVISAO");
                log.info("[segurança] Trilha de auditoria LIGADA: alterações em usuário, grupo, perfil, "
                        + "permissão, ação e recurso passam a ser registradas nas tabelas _AUD.");
                return;
            }
            // Desligar o Envers INTEIRO, não apenas os listeners.
            //
            // Só os listeners não bastava, e a diferença é exatamente a que este bean existe para
            // garantir: sem os listeners o Envers para de GRAVAR, mas continua CONTRIBUINDO o
            // mapeamento das tabelas _AUD. Com hibernate.ddl-auto=validate — o arranjo de quem
            // controla o schema por migrations, justamente quem esta chave protege — o Hibernate
            // então exige tabelas que ninguém pediu e a aplicação não sobe:
            //
            //     Schema validation: missing table [seguranca_acao_aud]
            //
            // Encontrado ao atualizar um consumidor da 3.1.1 para a 3.1.16: 177 testes de
            // integração pararam de carregar o contexto, todos por isto. A promessa escrita aqui
            // ("nenhuma tabela _AUD é exigida") era falsa desde que a trilha foi introduzida.
            propriedades.put(org.hibernate.envers.boot.internal.EnversService.INTEGRATION_ENABLED, "false");
            propriedades.put("hibernate.envers.autoRegisterListeners", "false");
            log.debug("[segurança] Trilha de auditoria desligada (archbase.security.audit.enabled=false). "
                    + "As entidades seguem anotadas, mas o Envers está inativo: nada é gravado e "
                    + "nenhuma tabela _AUD é mapeada ou exigida.");
        };
    }
}
