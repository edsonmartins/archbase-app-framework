package br.com.archbase.security.config;

import br.com.archbase.security.access.AccessLevel;
import br.com.archbase.security.spi.ArchbaseRoleResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Valida, na subida, se as proteções <b>habilitadas</b> têm seus pré-requisitos atendidos.
 *
 * <p><b>Por que existe.</b> Boa parte do endurecimento de segurança depende de um estado que o
 * framework não controla: um bean que a aplicação precisa registrar, linhas que precisam existir no
 * banco, uma migração que precisa ter rodado. Ligar a chave sem esse preparo não produz um erro
 * claro — produz um comportamento errado mais tarde, no meio de uma requisição: acesso negado a
 * quem deveria passar, sessão derrubada sem motivo aparente, credencial que some. O suporte que
 * nasce disso é caro e chega dias depois, longe da causa.
 *
 * <p>Então a checagem é feita uma vez, no lugar certo, e <b>falha a subida</b> com a instrução do
 * que fazer. Ligar uma proteção que não pode funcionar é um erro de configuração, e erro de
 * configuração deve aparecer no deploy, não em produção.
 *
 * <p>Para diagnóstico, dá para rebaixar a severidade — nunca como estado permanente:
 *
 * <pre>archbase.security.hardening.validation=warn   # ou: off</pre>
 */
@Component
@Slf4j
public class ArchbaseSecurityHardeningValidator {

    private static final String MODE_OFF = "off";
    private static final String MODE_WARN = "warn";

    /** Recursos usados pelos endpoints administrativos do próprio módulo de segurança. */
    private static final List<String> RECURSOS_ADMINISTRATIVOS = List.of(
            "USER", "GROUP", "RESOURCE", "ACTION", "USER_PROFILE", "API_TOKEN", "ACCESS_TOKEN");

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private List<ArchbaseRoleResolver> roleResolvers = List.of();

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    @Value("${archbase.security.hardening.validation:fail}")
    private String validationMode;

    @Value("${archbase.security.api-token.purge-plaintext:false}")
    private boolean purgePlaintext;

    @Value("${archbase.security.api-token.hash-enabled:true}")
    private boolean apiTokenHashEnabled;

    @Value("${archbase.security.require-role.no-resolver-policy:permit}")
    private String noResolverPolicy;

    @Value("${archbase.security.admin-endpoints.policy:permit}")
    private String adminEndpointsPolicy;

    @Value("${archbase.security.jwt.strict-token-use:false}")
    private boolean strictTokenUse;

    @Value("${archbase.security.logout.enabled:true}")
    private boolean logoutEnabled;

    @Value("${archbase.security.logout.url:/api/v1/auth/logout}")
    private String logoutUrl;

    @Value("${archbase.security.admin-guard.enabled:true}")
    private boolean adminGuardEnabled;

    @Value("${archbase.security.access-level.enabled:false}")
    private boolean accessLevelEnabled;

    @Value("${archbase.security.access-level.default:READER}")
    private String accessLevelDefault;

    @Value("${archbase.security.diagnostics.enabled:false}")
    private boolean diagnosticsEnabled;

    @Value("${archbase.security.admin-guard.allow-unverifiable-principal:false}")
    private boolean allowUnverifiablePrincipal;

    /**
     * Roda depois do {@code ArchbaseApiTokenHashMigrator}, que preenche os hashes na subida — a
     * checagem de {@code purge-plaintext} depende do resultado dele.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    @Transactional(readOnly = true)
    public void validate() {
        if (MODE_OFF.equalsIgnoreCase(validationMode)) {
            return;
        }

        List<String> bloqueios = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        validarPurgePlaintext(bloqueios);
        validarResolverDeRoles(bloqueios);
        validarPoliticaDeEndpointsAdministrativos(bloqueios);
        validarStrictTokenUse(bloqueios);
        validarConflitoDeRotaDoLogout(bloqueios);
        validarNivelDeAcesso(bloqueios, avisos);
        validarDiagnostico(avisos);
        coletarProtecoesInertes(avisos);

        avisos.forEach(aviso -> log.warn("[segurança] {}", aviso));

        if (bloqueios.isEmpty()) {
            log.info("[segurança] Pré-requisitos das proteções habilitadas: OK");
            return;
        }

        String mensagem = montarMensagem(bloqueios);
        if (MODE_WARN.equalsIgnoreCase(validationMode)) {
            log.error(mensagem);
            return;
        }
        throw new IllegalStateException(mensagem);
    }

    private String montarMensagem(List<String> bloqueios) {
        StringBuilder sb = new StringBuilder("\n\n")
                .append("═══ Configuração de segurança inconsistente ═══\n\n")
                .append("Proteções foram habilitadas sem os pré-requisitos atendidos. Cada item\n")
                .append("abaixo diz o que fazer. A aplicação não sobe até que sejam resolvidos, ou\n")
                .append("até que as chaves correspondentes voltem ao padrão.\n");
        for (int i = 0; i < bloqueios.size(); i++) {
            sb.append("\n  ").append(i + 1).append(") ").append(bloqueios.get(i)).append('\n');
        }
        sb.append("\nDetalhes de cada passo: deployment/security-hardening.md\n")
                .append("Para apenas registrar em log em vez de impedir a subida (diagnóstico):\n")
                .append("  archbase.security.hardening.validation=warn\n");
        return sb.toString();
    }

    private void validarPurgePlaintext(List<String> bloqueios) {
        if (!purgePlaintext) {
            return;
        }
        if (!apiTokenHashEnabled) {
            bloqueios.add("""
                    archbase.security.api-token.purge-plaintext=true com hash-enabled=false.
                       As duas se contradizem: uma manda apagar o valor em claro, a outra manda
                       continuar gravando-o. Escolha uma.""");
            return;
        }
        long pendentes = contar(
                "SELECT COUNT(*) FROM seguranca_token_api WHERE token_hash IS NULL AND token IS NOT NULL");
        if (pendentes > 0) {
            bloqueios.add(String.format("""
                    archbase.security.api-token.purge-plaintext=true, mas %d token(s) de API ainda
                       não têm hash calculado. Apagar o valor em claro agora deixaria essas
                       integrações sem meio de autenticar, de forma irreversível.
                       O que fazer: suba uma vez SEM purge-plaintext (a migração de hash roda na
                       inicialização), confirme no log a linha "hash calculado", e só então ligue.""",
                    pendentes));
        }
    }

    private void validarResolverDeRoles(List<String> bloqueios) {
        if (!"deny".equalsIgnoreCase(noResolverPolicy)) {
            return;
        }
        if (roleResolvers == null || roleResolvers.isEmpty()) {
            bloqueios.add("""
                    archbase.security.require-role.no-resolver-policy=deny, mas nenhum bean
                       ArchbaseRoleResolver foi registrado. Todo método anotado com @RequireRole
                       passaria a negar acesso — inclusive para quem tem a role.
                       O que fazer: implemente ArchbaseRoleResolver na aplicação (as roles de
                       @RequireRole são do seu domínio, o framework não tem como descobri-las) e
                       registre-o como @Component.""");
        }
    }

    private void validarPoliticaDeEndpointsAdministrativos(List<String> bloqueios) {
        if (!"permission".equalsIgnoreCase(adminEndpointsPolicy)) {
            return;
        }
        List<String> faltando = new ArrayList<>();
        for (String recurso : RECURSOS_ADMINISTRATIVOS) {
            long existe = contar(
                    "SELECT COUNT(*) FROM seguranca_recurso r "
                            + "JOIN seguranca_acao a ON a.id_recurso = r.id_recurso "
                            + "WHERE r.nome = :recurso AND a.nome = 'MANAGE'",
                    Map.of("recurso", recurso));
            if (existe == 0) {
                faltando.add(recurso);
            }
        }
        if (!faltando.isEmpty()) {
            bloqueios.add(String.format("""
                    archbase.security.admin-endpoints.policy=permission, mas não há Resource+Action
                       'MANAGE' cadastrados para: %s.
                       Sem esses registros ninguém consegue permissão nesses endpoints, nem um
                       administrador delegando — a gestão de usuários fica inacessível.
                       O que fazer: cadastre os recursos e a ação MANAGE (POST /api/v1/resource e
                       /api/v1/resource/register) antes de virar a chave; ou use
                       policy=admin-only, que não depende de cadastro nenhum.""",
                    String.join(", ", faltando)));
        }
    }

    private void validarStrictTokenUse(List<String> bloqueios) {
        if (!strictTokenUse) {
            return;
        }
        long legados = contar(
                "SELECT COUNT(*) FROM seguranca_token_acesso "
                        + "WHERE tp_uso_token IS NULL AND token_expirado = 'N' AND token_revogado = 'N'");
        if (legados > 0) {
            bloqueios.add(String.format("""
                    archbase.security.jwt.strict-token-use=true, mas existem %d sessão(ões) ativas
                       emitidas antes desta versão (sem o claim token_use). Ligar agora derruba
                       todas de uma vez.
                       O que fazer: espere o maior archbase.security.jwt.refresh-expiration passar
                       desde o deploy — as sessões antigas expiram sozinhas — e ligue depois. Este
                       contador chegando a zero é o sinal de que pode.""",
                    legados));
        }
    }

    private void validarConflitoDeRotaDoLogout(List<String> bloqueios) {
        if (!logoutEnabled || !existeEndpointMapeadoEm(logoutUrl)) {
            return;
        }
        {
            bloqueios.add(String.format("""
                    Existe um endpoint da aplicação mapeado em '%s', que é a URL do logout do
                       Archbase. O LogoutFilter do Spring intercepta antes do DispatcherServlet,
                       então esse seu endpoint deixaria de ser chamado — sem erro nenhum.
                       O que fazer: mude archbase.security.logout.url, ou desligue o logout do
                       framework com archbase.security.logout.enabled=false se a aplicação já
                       revoga os tokens por conta própria.""",
                    logoutUrl));
        }
    }

    /** Proteções que estão desligadas ou inertes — não impedem a subida, mas precisam ser vistas. */
    /**
     * O portão de nível ligado sem nível para comparar.
     *
     * <p>É a armadilha desta entrega: ligar {@code access-level.enabled} num sistema onde todos os
     * perfis têm {@code ACCESS_LEVEL} nulo joga todo mundo no nível padrão — e se o padrão for
     * {@code READER}, qualquer capacidade com mínimo acima disso passa a negar em massa, no primeiro
     * deploy, sem que ninguém tenha mudado permissão nenhuma.
     */
    private void validarNivelDeAcesso(List<String> bloqueios, List<String> avisos) {
        if (!accessLevelEnabled) {
            return;
        }

        AccessLevel padrao = AccessLevel.parse(accessLevelDefault);
        if (padrao == null) {
            bloqueios.add("archbase.security.access-level.default='" + accessLevelDefault
                    + "' não é um nível válido. Use READER, OPERATOR, SUPERVISOR ou TENANT_ADMIN.\n"
                    + "     O portão de nível está ligado (access-level.enabled=true) e não há como\n"
                    + "     saber que nível atribuir a quem não tem perfil.");
            return;
        }

        long perfisSemNivel = contar(
                "SELECT COUNT(*) FROM seguranca WHERE tp_seguranca = 'SEGURANCA_PERFIL' AND access_level IS NULL");
        long acoesComMinimo = contar(
                "SELECT COUNT(*) FROM seguranca_acao WHERE minimum_level IS NOT NULL");

        if (acoesComMinimo == 0) {
            avisos.add("archbase.security.access-level.enabled=true, mas nenhuma ação tem "
                    + "MINIMUM_LEVEL preenchido: o portão está ligado e não barra nada. "
                    + "Declare minimumLevel em @HasPermission ou preencha o mínimo no admin.");
            return;
        }

        if (perfisSemNivel > 0) {
            avisos.add(perfisSemNivel + " perfil(is) sem ACCESS_LEVEL, com o portão de nível ligado: "
                    + "essas pessoas caem no padrão " + padrao + ", e "
                    + acoesComMinimo + " ação(ões) têm mínimo declarado. "
                    + "Rode GET /api/v1/security/diagnostics/users/{id}/effective para ver quem perde o quê "
                    + "antes de manter isso em produção.");
        }
    }

    /**
     * O diagnóstico exposto sem administrador que o alcance.
     *
     * <p>Os endpoints exigem {@code isAdministrator}. Ligá-los sem nenhum administrador cadastrado
     * publica uma superfície que ninguém consegue usar — e que continua respondendo 403 a todo
     * mundo, o que costuma virar horas de investigação no lugar errado.
     */
    private void validarDiagnostico(List<String> avisos) {
        if (!diagnosticsEnabled) {
            return;
        }
        avisos.add("archbase.security.diagnostics.enabled=true: /api/v1/security/diagnostics/* está "
                + "publicado. Os endpoints exigem isAdministrator, mas revelam a estrutura de acesso "
                + "do tenant — mantenha ligado apenas enquanto durar a investigação.");
    }

    private void coletarProtecoesInertes(List<String> avisos) {
        if ("permit".equalsIgnoreCase(adminEndpointsPolicy)) {
            avisos.add("archbase.security.admin-endpoints.policy=permit: qualquer usuário "
                    + "autenticado alcança os endpoints administrativos de segurança "
                    + "(criar usuário, conceder permissão). Recomendado: admin-only.");
        }
        if (!adminGuardEnabled) {
            avisos.add("archbase.security.admin-guard.enabled=false: qualquer autenticado pode "
                    + "criar ou promover administrador.");
        }
        if (allowUnverifiablePrincipal) {
            avisos.add("archbase.security.admin-guard.allow-unverifiable-principal=true: o "
                    + "privilégio do solicitante deixa de ser verificado quando o principal não é "
                    + "UserEntity. Prefira fazer o UserDetailsService devolver UserEntity.");
        }
        if (!apiTokenHashEnabled) {
            avisos.add("archbase.security.api-token.hash-enabled=false: tokens de API continuam "
                    + "gravados em texto puro no banco.");
        }
    }

    /**
     * Verifica por reflexão se a aplicação mapeou algum endpoint MVC no caminho informado.
     *
     * <p>Reflexão porque {@code archbase-security} não depende de {@code spring-webmvc}, e criar
     * essa dependência só para uma checagem de diagnóstico acoplaria o módulo de segurança à stack
     * web. Melhor esforço: se não der para inspecionar, a checagem é pulada em silêncio.
     */
    @SuppressWarnings("unchecked")
    private boolean existeEndpointMapeadoEm(String url) {
        if (applicationContext == null) {
            return false;
        }
        try {
            Object mapping = applicationContext.getBean("requestMappingHandlerMapping");
            Map<Object, Object> handlerMethods = (Map<Object, Object>) mapping.getClass()
                    .getMethod("getHandlerMethods").invoke(mapping);
            for (Object info : handlerMethods.keySet()) {
                Collection<String> patterns = (Collection<String>) info.getClass()
                        .getMethod("getPatternValues").invoke(info);
                if (patterns.contains(url)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Não foi possível inspecionar os mapeamentos MVC: {}", e.getMessage());
        }
        return false;
    }

    private long contar(String sql) {
        return contar(sql, Map.of());
    }

    private long contar(String sql, Map<String, Object> parametros) {
        try {
            var query = entityManager.createNativeQuery(sql);
            parametros.forEach(query::setParameter);
            Object resultado = query.getSingleResult();
            return resultado != null ? Long.parseLong(resultado.toString()) : 0L;
        } catch (Exception e) {
            // Tabela ausente ou schema ainda não migrado: não é o que este validador apura, e
            // transformar isso em falha esconderia o erro real de schema com uma mensagem errada.
            log.debug("Não foi possível executar a checagem de pré-requisito: {}", e.getMessage());
            return 0L;
        }
    }
}
