package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.access.AccessSubject;
import br.com.archbase.security.access.ArchbaseAccessSubjectLoader;
import br.com.archbase.security.access.ArchbaseCapabilityReader;
import br.com.archbase.security.access.EffectiveCapability;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import br.com.archbase.security.adapter.port.ResourcePersistencePort;
import br.com.archbase.security.domain.dto.*;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.*;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import br.com.archbase.security.repository.ResourceJpaRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ResourcePersistenceAdapter implements ResourcePersistencePort, FindDataWithFilterQuery<String, ResourceDto> {

    private final ResourceJpaRepository repository;
    private final SecurityAdapter securityAdapter;
    private final PermissionJpaRepository permissionRepository;
    private final ArchbaseAccessSubjectLoader subjectLoader;
    private final ArchbaseCapabilityReader capabilityReader;
    private final JPAQueryFactory queryFactory;

    @Autowired
    public ResourcePersistenceAdapter(ResourceJpaRepository repository, SecurityAdapter securityAdapter,
                                      PermissionJpaRepository permissionRepository,
                                      ArchbaseAccessSubjectLoader subjectLoader,
                                      ArchbaseCapabilityReader capabilityReader,
                                      EntityManager entityManager) {
        this.repository = repository;
        this.securityAdapter = securityAdapter;
        this.permissionRepository = permissionRepository;
        this.subjectLoader = subjectLoader;
        this.capabilityReader = capabilityReader;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<ResourceDto> findAllResources() {
        return repository.findAll().stream().map(ResourceEntity::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<ResourceDto> findResourceById(String id) {
        return repository.findById(id).map(ResourceEntity::toDto);
    }

    @Override
    public ResourceDto createResource(ResourceDto resourceDto) {
        return repository.save(ResourceEntity.fromDomain(resourceDto.toDomain())).toDto();
    }

    @Override
    public Optional<ResourceDto> updateResource(String id, ResourceDto resourceDto) {
        return Optional.of(repository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(existingEntity -> {
                    existingEntity.setDescription(resourceDto.getDescription());
                    existingEntity.setActive(resourceDto.getActive());
                    existingEntity.setName(resourceDto.getName());
                    return repository.save(existingEntity).toDto();
                });
    }

    @Override
    public void deleteResource(String id) {
        repository.deleteById(id);
    }

    @Override
    public ResourceDto findResource(String resourceName) {
        QResourceEntity resource = QResourceEntity.resourceEntity;
        ResourceEntity resourceEntity = queryFactory.selectFrom(resource).where(resource.name.eq(resourceName)).fetchOne();
        if (resourceEntity == null) {
            return null;
        }
        return resourceEntity.toDto();
    }

    @Override
    /**
     * As capacidades do usuário logado sobre um recurso — o que a tela consome para mostrar ou
     * esconder botão, menu e aba.
     *
     * <p>Passou a usar o {@link ArchbaseCapabilityReader}, que é a mesma leitura do diagnóstico e
     * do core. Antes, isto era uma segunda implementação de usuário ∪ grupos ∪ perfil em QueryDSL,
     * independente da JPQL que o {@code @HasPermission} usa — e com regra diferente: filtrava
     * {@code action.active}, coisa que o outro caminho não faz. Duas respostas para a mesma
     * pergunta é como um sistema acaba concedendo mais do que a interface mostra.
     *
     * <p>O filtro preservado é exatamente o anterior: <b>{@code action.active}</b>. Recurso inativo
     * continua não sendo filtrado aqui, como nunca foi — apertar isso é mudança de comportamento e
     * pertence à flag {@code archbase.security.permission.require-active}, não a este passo.
     *
     * <p>Deixou de depender de sessão aberta: o sujeito é carregado com fetch join de grupos e
     * perfil, em vez de converter a entidade tocando associação lazy.
     */
    public ResourcePermissionsDto findLoggedUserResourcePermissions(String resourceName) {
        AccessSubject subject = subjectLoader.byId(loggedUserId())
                .orElseThrow(() -> new ArchbaseValidationException("Usuário não encontrado."));

        Set<String> acoes = capabilityReader.grantedTo(subject, resourceName).stream()
                // Negada não entra: a tela mostraria um botão que o backend recusa.
                .filter(c -> c.situation() != EffectiveCapability.Situation.DENIED)
                .filter(EffectiveCapability::actionActive)
                .map(EffectiveCapability::action)
                .collect(Collectors.toSet());

        return ResourcePermissionsDto.builder()
                .resourceName(resourceName)
                .permissions(acoes)
                .build();
    }

    /**
     * Todas as capacidades do usuário autenticado, agrupadas por recurso.
     *
     * <p>Uma consulta, não uma por recurso. Quem monta menu ou roteamento precisa da resposta para
     * dezenas de recursos ao mesmo tempo, e o caminho por recurso transformava isso em dezenas de
     * requisições — na prática, em desistir de perguntar.
     *
     * <p><b>O filtro é o mesmo</b> de {@link #findLoggedUserResourcePermissions(String)}: fora as
     * negadas, fora as de ação inativa. Deliberadamente idêntico, e não "mais correto": se as duas
     * listagens divergissem, o menu habilitaria um item cuja tela recusaria as ações — a mesma
     * classe de divergência entre interface e decisão que o core existe para eliminar.
     */
    @Override
    public LoggedUserPermissionsDto findLoggedUserPermissions() {
        AccessSubject subject = subjectLoader.byId(loggedUserId())
                .orElseThrow(() -> new ArchbaseValidationException("Usuário não encontrado."));

        Map<String, Set<String>> porRecurso = new LinkedHashMap<>();
        for (EffectiveCapability capacidade : capabilityReader.grantedTo(subject)) {
            if (capacidade.situation() == EffectiveCapability.Situation.DENIED || !capacidade.actionActive()) {
                continue;
            }
            porRecurso.computeIfAbsent(capacidade.resource(), r -> new LinkedHashSet<>())
                    .add(capacidade.action());
        }

        return LoggedUserPermissionsDto.builder()
                .administrator(Boolean.TRUE.equals(subject.administrator()))
                .permissions(porRecurso)
                .build();
    }

    /**
     * O id do usuário autenticado, sem converter a entidade para domínio.
     *
     * <p>{@code SecurityAdapter.getLoggedUser()} faria uma ida ao banco e um {@code toDomain()} que
     * toca {@code groups}, associação lazy — o que só funciona dentro de uma requisição web, com a
     * sessão que o Open Session In View mantém aberta.
     */
    /**
     * Exclui as negações das listagens do admin.
     *
     * <p>Estas consultas respondem "o que foi concedido a esta pessoa/grupo/perfil". Uma linha
     * {@code DENY} listada aí afirma o oposto do que o avaliador decide — e é a mesma divergência
     * entre tela e decisão que o core existe para eliminar. Exibir as negações para que o admin
     * possa removê-las é útil, mas exige que o DTO carregue o efeito; enquanto não carregar,
     * mostrá-las é mentir.
     */
    private BooleanExpression naoENegacao(QPermissionEntity permission) {
        return permission.effect.isNull()
                .or(permission.effect.ne(br.com.archbase.security.access.PermissionEffect.DENY));
    }

    private String loggedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new ArchbaseValidationException("Usuário não autenticado.");
        }
        return user.getId();
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findUserResourcesPermissions(String userId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QUserEntity user = QUserEntity.userEntity;
        QUserGroupEntity userGroup = QUserGroupEntity.userGroupEntity;
        QGroupEntity group = QGroupEntity.groupEntity;
        QProfileEntity profile = QProfileEntity.profileEntity;

        List<Tuple> userPermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.USER), permission.id)
                .from(permission)
                .where(permission.security.id.eq(userId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch();

        List<Tuple> profilePermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.PROFILE))
                .from(permission)
                .join(permission.security, profile._super)
                .join(user).on(user.profile.eq(profile))
                .where(user.id.eq(userId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch();

        List<Tuple> groupPermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.GROUP))
                .from(permission)
                .join(permission.security, group._super)
                .join(userGroup).on(userGroup.group.eq(group))
                .where(userGroup.user.id.eq(userId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch();

        List<Tuple> permissionsTuple = new ArrayList<>();
        permissionsTuple.addAll(userPermissions);
        permissionsTuple.addAll(profilePermissions);
        permissionsTuple.addAll(groupPermissions);

        return groupTuplesToResourcePermissions(permissionsTuple, permission);
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findProfileResourcesPermissions(String profileId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QProfileEntity profile = QProfileEntity.profileEntity;

        List<Tuple> profilePermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.PROFILE), permission.id)
                .from(permission)
                .join(permission.security, profile._super)
                .where(profile.id.eq(profileId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch();

        return groupTuplesToResourcePermissions(profilePermissions, permission);
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findGroupResourcesPermissions(String groupId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QGroupEntity group = QGroupEntity.groupEntity;

        List<Tuple> groupPermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.GROUP), permission.id)
                .from(permission)
                .join(permission.security, group._super)
                .where(group.id.eq(groupId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch();


        return groupTuplesToResourcePermissions(groupPermissions, permission);
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findAllResourcesPermissions() {
        QActionEntity action = QActionEntity.actionEntity;

        List<Tuple> permissionsTuple = queryFactory
                .select(action.resource.id, action.resource.description, action.id, action.description)
                .from(action)
                .where(action.active.isTrue())
                .fetch();

        Map<String, Map<String, List<Tuple>>> groupedByResource = permissionsTuple.stream()
                .collect(Collectors.groupingBy(t -> t.get(action.resource.id) + ":" + t.get(action.resource.description),
                        Collectors.groupingBy(t -> t.get(action.id))));

        return groupedByResource.entrySet().stream()
                .map(entry -> {
                    List<String> resourceIdName = Arrays.stream(entry.getKey().split(":")).toList();
                    List<PermissionWithTypesDto> permissions = entry.getValue().entrySet().stream()
                            .map(actionEntry -> {
                                String actionId = actionEntry.getKey();
                                String actionName = actionEntry.getValue().get(0).get(action.description);
                                return PermissionWithTypesDto.builder()
                                        .actionDescription(actionName)
                                        .actionId(actionId)
                                        .build();
                            })
                            .collect(Collectors.toList());
                    return ResoucePermissionsWithTypeDto.builder()
                            .resourceId(resourceIdName.get(0))
                            .resourceDescription(resourceIdName.get(1))
                            .permissions(permissions)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static List<ResoucePermissionsWithTypeDto> groupTuplesToResourcePermissions(List<Tuple> permissionsTuple, QPermissionEntity permission) {
        Map<String, Map<String, List<Tuple>>> groupedByResource = permissionsTuple.stream()
                .collect(Collectors.groupingBy(t -> t.get(permission.action.resource.id) + ":" + t.get(permission.action.resource.description),
                        Collectors.groupingBy(t -> t.get(permission.action.id))));

        return groupedByResource.entrySet().stream()
                .map(entry -> {
                    List<String> resourceIdDescription = Arrays.stream(entry.getKey().split(":")).toList();
                    List<PermissionWithTypesDto> permissions = entry.getValue().entrySet().stream()
                            .map(actionEntry -> {
                                String actionId = actionEntry.getKey();
                                Set<SecurityType> types = actionEntry.getValue().stream()
                                        .map(t -> t.get(4, SecurityType.class))
                                        .collect(Collectors.toSet());
                                String actionDescription = actionEntry.getValue().get(0).get(permission.action.description);
                                String permissionId = actionEntry.getValue().get(0).get(permission.id);
                                PermissionWithTypesDto permissionWithTypesDto = PermissionWithTypesDto.builder()
                                        .actionDescription(actionDescription)
                                        .actionId(actionId)
                                        .types(types)
                                        .build();

                                if (permissionId != null) {
                                    permissionWithTypesDto.setPermissionId(permissionId);
                                }

                                return permissionWithTypesDto;
                            })
                            .collect(Collectors.toList());
                    return ResoucePermissionsWithTypeDto.builder()
                            .resourceId(resourceIdDescription.get(0))
                            .resourceDescription(resourceIdDescription.get(1))
                            .permissions(permissions)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deletePermission(String id) {
        permissionRepository.deleteById(id);
    }

    @Override
    public PermissionDto grantPermission(PermissionDto permissionDto) {
        // O efeito atravessa DTO → domínio → entidade como todos os demais campos. Uma versão
        // anterior o atribuía direto na entidade, depois do fromDomain: funcionava, e deixava um
        // campo fora do caminho que todos os outros percorrem — o tipo de exceção que o próximo a
        // mexer aqui não tem como adivinhar.
        return permissionRepository.save(
                PermissionEntity.fromDomain(permissionDto.toDomain())).toDto();
    }

    @Override
    public PermissionDto findPermission(String securityId, String actionId) {
        QPermissionEntity permission = QPermissionEntity.permissionEntity;

        PermissionEntity permissionEntity = queryFactory.selectFrom(permission)
                .where(permission.action.id.eq(actionId).and(permission.security.id.eq(securityId)))
                .fetchFirst();
        if (permissionEntity == null) {
            return null;
        }
        return permissionEntity.toDto();
    }

    @Override
    public ResourceDto findById(String id) {
        Optional<ResourceEntity> byId = repository.findById(id);
        return byId.map(ResourceEntity::toDto).orElse(null);
    }

    @Override
    public Page<ResourceDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ResourceEntity> result = repository.findAll(pageable);
        List<ResourceDto> list = result.stream().map(ResourceEntity::toDto).toList();
        return new ResourcePersistenceAdapter.PageResource(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ResourceDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ResourceEntity> result = repository.findAll(pageable);
        List<ResourceDto> list = result.stream().map(ResourceEntity::toDto).toList();
        return new ResourcePersistenceAdapter.PageResource(list, pageable, result.getTotalElements());
    }

    @Override
    public List<ResourceDto> findAll(List<String> ids) {
        List<ResourceEntity> result = repository.findAllById(ids);
        return result.stream().map(ResourceEntity::toDto).toList();
    }

    @Override
    public Page<ResourceDto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ResourceEntity> result = repository.findAll(filter, pageable);
        List<ResourceDto> list = result.stream().map(ResourceEntity::toDto).toList();
        return new ResourcePersistenceAdapter.PageResource(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ResourceDto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ResourceEntity> result = repository.findAll(filter, pageable);
        List<ResourceDto> list = result.stream().map(ResourceEntity::toDto).toList();
        return new ResourcePersistenceAdapter.PageResource(list, pageable, result.getTotalElements());
    }

    static class PageResource extends PageImpl<ResourceDto> {
        public PageResource(List<ResourceDto> content) {
            super(content);
        }

        public PageResource(List<ResourceDto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    static class ListResource extends ArrayList<ResourceDto> {
        public ListResource(Collection<? extends ResourceDto> c) {
            super(c);
        }
    }
}
