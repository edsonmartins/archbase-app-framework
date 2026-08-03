package br.com.archbase.security.diagnostics;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.access.AccessSubject;
import br.com.archbase.security.access.ArchbaseAccessEvaluator;
import br.com.archbase.security.access.ArchbaseAccessSubjectLoader;
import br.com.archbase.security.access.ArchbaseCapabilityReader;
import br.com.archbase.security.access.EffectiveCapability;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.GroupJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.ProfileJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.spi.ArchbaseRoleResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Responde as perguntas que a tela de segurança não conseguia responder.
 *
 * <p>Todas as três — "o que esta pessoa pode?", "ela conseguiria fazer isto?", "como está o
 * conjunto?" — passam pelo <b>mesmo</b> {@link ArchbaseAccessEvaluator} que decide em produção. É
 * a razão de o core existir: se o diagnóstico tivesse motor próprio, ele divergiria da realidade
 * com o tempo, e um diagnóstico que mente é pior do que nenhum.
 */
@Service
public class ArchbaseAccessDiagnosticsService {

    private final ArchbaseAccessEvaluator evaluator;
    private final ArchbaseAccessSubjectLoader subjectLoader;
    private final ArchbaseCapabilityReader capabilityReader;
    private final PermissionJpaRepository permissionRepository;
    private final ActionJpaRepository actionRepository;
    private final ResourceJpaRepository resourceRepository;
    private final UserJpaRepository userRepository;
    private final GroupJpaRepository groupRepository;
    private final ProfileJpaRepository profileRepository;

    @Autowired(required = false)
    private List<ArchbaseRoleResolver> roleResolvers = List.of();

    @Value("${archbase.security.permission.require-active:false}")
    private boolean requireActive;

    @Value("${archbase.security.scan-packages:}")
    private String scanPackages;

    @Value("${archbase.security.admin-endpoints.policy:permit}")
    private String adminEndpointsPolicy;

    @Value("${archbase.security.require-role.no-resolver-policy:permit}")
    private String requireRoleNoResolverPolicy;

    public ArchbaseAccessDiagnosticsService(ArchbaseAccessEvaluator evaluator,
                                            ArchbaseAccessSubjectLoader subjectLoader,
                                            ArchbaseCapabilityReader capabilityReader,
                                            PermissionJpaRepository permissionRepository,
                                            ActionJpaRepository actionRepository,
                                            ResourceJpaRepository resourceRepository,
                                            UserJpaRepository userRepository,
                                            GroupJpaRepository groupRepository,
                                            ProfileJpaRepository profileRepository) {
        this.evaluator = evaluator;
        this.subjectLoader = subjectLoader;
        this.capabilityReader = capabilityReader;
        this.permissionRepository = permissionRepository;
        this.actionRepository = actionRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.profileRepository = profileRepository;
    }

    // ------------------------------------------------------------------ simulação

    /**
     * "Esta pessoa conseguiria fazer isto?" — avaliada, não executada.
     *
     * <p>Chama o avaliador de produção com o sujeito pedido. A resposta traz o portão em que parou
     * e o motivo, que é o que hoje obriga a abrir grupo por grupo.
     */
    @Transactional(readOnly = true)
    public Optional<AccessDecision> simulate(String userId, String email, AccessRequirement requirement) {
        return subjectLoader.resolve(userId, email)
                .map(subject -> evaluator.decide(subject, requirement));
    }

    // ------------------------------------------------------------------ efetivo

    /** "O que esta pessoa pode?" — a lista achatada, com origem e situação por linha. */
    @Transactional(readOnly = true)
    public Optional<EffectiveAccessReport> effective(String userId, String email) {
        return subjectLoader.resolve(userId, email).map(this::effectiveOf);
    }

    private EffectiveAccessReport effectiveOf(AccessSubject subject) {
        List<EffectiveCapability> capacidades = capabilityReader.grantedTo(subject);
        int inertes = (int) capacidades.stream()
                .filter(c -> c.situation() == EffectiveCapability.Situation.INERT).count();

        // Nomes, não identificadores: este relatório existe para dispensar a ida ao admin.
        List<String> grupos = subject.groupNames().stream().sorted().toList();

        return new EffectiveAccessReport(
                subject.userId(),
                subject.label(),
                subject.profileName(),
                grupos,
                subject.isAdministrator(),
                subject.enabled(),
                capacidades.size(),
                capacidades.size() - inertes,
                inertes,
                capacidades);
    }


    // ------------------------------------------------------------------ panorama

    /** "Como está o conjunto?" — os números do tenant corrente. */
    @Transactional(readOnly = true)
    public AccessOverview overview() {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        for (Object[] linha : permissionRepository.countGroupedBySecurityType()) {
            Object tipo = linha[0];
            String nome = tipo instanceof Class<?> classe
                    ? classe.getSimpleName().replace("Entity", "")
                    : String.valueOf(tipo);
            porTipo.merge(nome, ((Number) linha[1]).longValue(), Long::sum);
        }

        long usuarios = userRepository.count();
        long administradores = userRepository.countAdministrators();

        return new AccessOverview(
                usuarios,
                administradores,
                groupRepository.count(),
                profileRepository.count(),
                resourceRepository.count(),
                resourceRepository.countByType(TipoRecurso.API),
                resourceRepository.countByTypeAndActive(TipoRecurso.API, false),
                resourceRepository.countWithoutAnyAction(),
                actionRepository.count(),
                actionRepository.countByActive(false),
                actionRepository.countWithoutAnyPermission(),
                permissionRepository.countAll(),
                permissionRepository.countPointingToInactive(),
                porTipo,
                new AccessOverview.Flags(
                        requireActive,
                        scanPackages != null && !scanPackages.isBlank(),
                        adminEndpointsPolicy,
                        requireRoleNoResolverPolicy,
                        roleResolvers != null && !roleResolvers.isEmpty()));
    }
}
