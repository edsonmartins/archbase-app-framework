package br.com.archbase.security.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * De onde sai o nível de uma pessoa, e se o portão {@link Gate#LEVEL} vale.
 *
 * <p>A resolução tem ordem, e a primeira resposta ganha:
 *
 * <ol>
 *   <li><b>Administrador</b> — {@link AccessLevel#TENANT_ADMIN}, por definição. É o topo da escala,
 *       então o portão nunca barra um administrador. Não há flag para isso: uma capacidade não pode
 *       exigir mais do que o degrau mais alto.</li>
 *   <li><b>{@link ArchbaseAccessLevelResolver} da aplicação</b>, se registrado — para quem modela
 *       senioridade fora do perfil.</li>
 *   <li><b>O perfil do usuário</b>, que é a fonte padrão.</li>
 *   <li><b>{@code archbase.security.access-level.default}</b>, para quem não tem perfil ou tem um
 *       perfil sem nível — que é o estado de todo perfil existente no dia em que a coluna entra.</li>
 * </ol>
 *
 * <p>O portão só é avaliado com {@code archbase.security.access-level.enabled=true}. Desligado — o
 * padrão — nada disto é consultado, e o comportamento é o de antes.
 */
@Component
public class ArchbaseAccessLevelPolicy {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseAccessLevelPolicy.class);

    /**
     * Lista, e não bean único: uma aplicação modular pode registrar um resolver por módulo, e
     * injetar o tipo direto derrubaria a subida com {@code NoUniqueBeanDefinitionException}.
     */
    @Autowired(required = false)
    private List<ArchbaseAccessLevelResolver> resolvers = List.of();

    @Value("${archbase.security.access-level.enabled:false}")
    private boolean enabled;

    @Value("${archbase.security.access-level.default:READER}")
    private String defaultLevel;

    public boolean isEnabled() {
        return enabled;
    }

    /** O nível deste sujeito, resolvido pela ordem acima. Nunca devolve {@code null}. */
    public AccessLevel levelOf(AccessSubject subject) {
        if (subject == null) {
            return padrao();
        }

        if (subject.isAdministrator()) {
            return AccessLevel.TENANT_ADMIN;
        }

        if (resolvers != null) {
            for (ArchbaseAccessLevelResolver resolver : resolvers) {
                AccessLevel resolvido = resolver.resolveLevel(subject);
                if (resolvido != null) {
                    return resolvido;
                }
            }
        }

        return subject.level() != null ? subject.level() : padrao();
    }

    /**
     * O nível de quem não tem perfil, ou tem perfil sem nível.
     *
     * <p>Valor inválido na configuração cai em {@link AccessLevel#READER} com aviso — e não no
     * degrau mais alto, que transformaria um erro de digitação em liberação geral.
     */
    private AccessLevel padrao() {
        AccessLevel nivel = AccessLevel.parse(defaultLevel);
        if (nivel == null) {
            log.warn("Valor inválido para archbase.security.access-level.default: '{}'. "
                    + "Use READER, OPERATOR, SUPERVISOR ou TENANT_ADMIN. Assumindo READER.", defaultLevel);
            return AccessLevel.READER;
        }
        return nivel;
    }
}
