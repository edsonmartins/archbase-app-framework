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
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.*;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import br.com.archbase.security.repository.ResourceJpaRepository;
import com.querydsl.core.Tuple;
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
    private final br.com.archbase.security.repository.ActionDependencyJpaRepository dependencyRepository;
    private final JPAQueryFactory queryFactory;

    @Autowired
    public ResourcePersistenceAdapter(ResourceJpaRepository repository, SecurityAdapter securityAdapter,
                                      PermissionJpaRepository permissionRepository,
                                      ArchbaseAccessSubjectLoader subjectLoader,
                                      ArchbaseCapabilityReader capabilityReader,
                                      br.com.archbase.security.repository.ActionDependencyJpaRepository dependencyRepository,
                                      EntityManager entityManager) {
        this.repository = repository;
        this.securityAdapter = securityAdapter;
        this.permissionRepository = permissionRepository;
        this.subjectLoader = subjectLoader;
        this.capabilityReader = capabilityReader;
        this.dependencyRepository = dependencyRepository;
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

    /**
     * Classifica um recurso que esteja <b>sem tipo</b>, e nada mais.
     *
     * <p>Existe porque {@link #updateResource(String, ResourceDto)} não copia o tipo — e não deve
     * passar a copiar. Aquele método atende o {@code PUT /api/v1/resource/{id}}, que todo cliente
     * atual chama sem o campo; copiar o que chega <b>apagaria</b> a classificação a cada edição de
     * descrição feita pelo admin.
     *
     * <p>Grava só quando o tipo está nulo. Recurso já classificado é devolvido intacto, para que a
     * classificação não oscile entre a subida da aplicação (que marca {@code API}) e a abertura da
     * tela (que marca {@code VIEW}) — um recurso que troca de tipo troca de seção na interface a
     * cada deploy.
     */
    public Optional<ResourceDto> classificarSeSemTipo(String id, TipoRecurso tipo) {
        return repository.findById(id).map(entidade -> {
            if (entidade.getType() != null) {
                return entidade.toDto();
            }
            entidade.setType(tipo);
            entidade.setUpdateEntityDate(java.time.LocalDateTime.now());
            entidade.setLastModifiedByUser("archbase");
            return repository.save(entidade).toDto();
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

    /**
     * Uma linha crua do catálogo, já com a origem resolvida.
     *
     * <p>Substitui o par de gambiarras que sustentava este trecho: a origem era lida do
     * {@code Tuple} por <b>posição</b> ({@code t.get(4, SecurityType.class)}), o que amarrava o
     * agrupamento à ordem das colunas do {@code select} — acrescentar uma coluna quebrava a leitura
     * sem erro de compilação —, e a identidade do recurso era transportada concatenando
     * {@code id + ":" + descrição} para depois ser desfeita com {@code split(":")}. A segunda era
     * pior que feia: descrição contendo {@code :} voltava truncada, e descrição <b>vazia</b> fazia
     * {@code "id:".split(":")} devolver um único elemento, de modo que o {@code get(1)} estourava
     * {@code IndexOutOfBounds} e a tela inteira respondia 500. Recurso de tela nasce com a descrição
     * que o cliente mandar, e o registro aceita string vazia.
     */
    private record LinhaDePermissao(
            String resourceId,
            String resourceName,
            String resourceDescription,
            TipoRecurso resourceType,
            Boolean resourceActive,
            String actionId,
            String actionName,
            String actionLabel,
            String actionDescription,
            String actionCategory,
            SecurityType origem,
            String permissionId) {
    }

    /**
     * As colunas que as três consultas selecionam, na mesma ordem — o que permite uma única
     * conversão para {@link LinhaDePermissao}.
     */
    private static com.querydsl.core.types.Expression<?>[] colunasDaPermissao(QPermissionEntity permission) {
        return new com.querydsl.core.types.Expression<?>[]{
                permission.action.resource.id,
                permission.action.resource.name,
                permission.action.resource.description,
                permission.action.resource.type,
                permission.action.resource.active,
                permission.action.id,
                permission.action.name,
                permission.action.label,
                permission.action.description,
                permission.action.category,
                permission.id
        };
    }

    /**
     * Converte a tupla numa linha, com a origem e a concessão declaradas por quem chamou.
     *
     * @param comConcessao {@code false} para as vias <b>herdadas</b>. Ao editar um usuário, a
     *                     concessão que vem do grupo ou do perfil não é dele: devolver o
     *                     {@code permissionId} daquela linha faria o botão de remover apagar a
     *                     permissão <b>do grupo inteiro</b> a partir da tela de uma pessoa. O nulo
     *                     é o que sinaliza "isto você não tira daqui" — e é responsabilidade da
     *                     interface dizer isso, em vez de apenas desabilitar o botão.
     */
    private static LinhaDePermissao linhaDe(Tuple tupla, QPermissionEntity permission,
                                            SecurityType origem, boolean comConcessao) {
        return new LinhaDePermissao(
                tupla.get(permission.action.resource.id),
                tupla.get(permission.action.resource.name),
                tupla.get(permission.action.resource.description),
                tupla.get(permission.action.resource.type),
                tupla.get(permission.action.resource.active),
                tupla.get(permission.action.id),
                tupla.get(permission.action.name),
                tupla.get(permission.action.label),
                tupla.get(permission.action.description),
                tupla.get(permission.action.category),
                origem,
                comConcessao ? tupla.get(permission.id) : null);
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findUserResourcesPermissions(String userId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QUserEntity user = QUserEntity.userEntity;
        QUserGroupEntity userGroup = QUserGroupEntity.userGroupEntity;
        QGroupEntity group = QGroupEntity.groupEntity;
        QProfileEntity profile = QProfileEntity.profileEntity;

        List<Tuple> userPermissions = queryFactory
                .select(colunasDaPermissao(permission))
                .from(permission)
                .where(permission.security.id.eq(userId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch();

        List<Tuple> profilePermissions = queryFactory
                .select(colunasDaPermissao(permission))
                .from(permission)
                .join(permission.security, profile._super)
                .join(user).on(user.profile.eq(profile))
                .where(user.id.eq(userId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch();

        List<Tuple> groupPermissions = queryFactory
                .select(colunasDaPermissao(permission))
                .from(permission)
                .join(permission.security, group._super)
                .join(userGroup).on(userGroup.group.eq(group))
                .where(userGroup.user.id.eq(userId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch();

        List<LinhaDePermissao> linhas = new ArrayList<>();
        userPermissions.forEach(t -> linhas.add(linhaDe(t, permission, SecurityType.USER, true)));
        profilePermissions.forEach(t -> linhas.add(linhaDe(t, permission, SecurityType.PROFILE, false)));
        groupPermissions.forEach(t -> linhas.add(linhaDe(t, permission, SecurityType.GROUP, false)));

        return comDependencias(agruparPorRecurso(linhas));
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findProfileResourcesPermissions(String profileId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QProfileEntity profile = QProfileEntity.profileEntity;

        List<LinhaDePermissao> linhas = queryFactory
                .select(colunasDaPermissao(permission))
                .from(permission)
                .join(permission.security, profile._super)
                .where(profile.id.eq(profileId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch()
                .stream()
                .map(t -> linhaDe(t, permission, SecurityType.PROFILE, true))
                .toList();

        return comDependencias(agruparPorRecurso(linhas));
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findGroupResourcesPermissions(String groupId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QGroupEntity group = QGroupEntity.groupEntity;

        List<LinhaDePermissao> linhas = queryFactory
                .select(colunasDaPermissao(permission))
                .from(permission)
                .join(permission.security, group._super)
                .where(group.id.eq(groupId).and(permission.action.active.isTrue()).and(naoENegacao(permission)))
                .fetch()
                .stream()
                .map(t -> linhaDe(t, permission, SecurityType.GROUP, true))
                .toList();

        return comDependencias(agruparPorRecurso(linhas));
    }

    /**
     * O catálogo inteiro — a lista "disponíveis" da tela de concessão.
     *
     * <p>Parte de {@code ActionEntity}, e não de {@code PermissionEntity}: a pergunta aqui é o que
     * <b>existe para ser concedido</b>, não o que já foi. Recurso sem nenhuma ação ativa não aparece,
     * o que é correto — não há o que conceder nele.
     *
     * <p>O filtro continua sendo apenas {@code action.active}. Recurso inativo com ação ativa segue
     * listado, como sempre esteve: apertar isso é mudança de comportamento e pertence à flag
     * {@code archbase.security.permission.require-active}, não a este passo.
     */
    @Override
    public List<ResoucePermissionsWithTypeDto> findAllResourcesPermissions() {
        QActionEntity action = QActionEntity.actionEntity;

        List<LinhaDePermissao> linhas = queryFactory
                .select(action.resource.id, action.resource.name, action.resource.description,
                        action.resource.type, action.resource.active, action.id, action.name,
                        action.label, action.description, action.category)
                .from(action)
                .where(action.active.isTrue())
                .fetch()
                .stream()
                .map(t -> new LinhaDePermissao(
                        t.get(action.resource.id),
                        t.get(action.resource.name),
                        t.get(action.resource.description),
                        t.get(action.resource.type),
                        t.get(action.resource.active),
                        t.get(action.id),
                        t.get(action.name),
                        t.get(action.label),
                        t.get(action.description),
                        t.get(action.category),
                        // Sem origem: esta lista diz o que EXISTE, não o que foi concedido a
                        // alguém. Inventar um SecurityType aqui faria a tela marcar como concedido
                        // o catálogo inteiro.
                        null,
                        null))
                .toList();

        return comDependencias(agruparPorRecurso(linhas));
    }

    /**
     * Preenche as dependências diretas de cada capacidade da resposta.
     *
     * <p>Uma consulta para a listagem inteira, e não uma por linha: a tela de concessão baixa o
     * catálogo completo a cada abertura, e uma consulta por capacidade transformaria isso em
     * centenas de idas ao banco.
     *
     * <p>Nulo quando não há nenhuma — o DTO omite o campo, e o cliente que não o conhece continua
     * recebendo exatamente a resposta de antes.
     */
    private List<ResoucePermissionsWithTypeDto> comDependencias(List<ResoucePermissionsWithTypeDto> recursos) {
        List<String> capacidades = recursos.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(PermissionWithTypesDto::getActionId)
                .filter(Objects::nonNull)
                .toList();

        if (capacidades.isEmpty()) {
            return recursos;
        }

        Map<String, List<String>> porCapacidade = new LinkedHashMap<>();
        dependencyRepository.findCapabilitiesRequiredBy(capacidades).forEach(linha ->
                porCapacidade.computeIfAbsent((String) linha[0], a -> new ArrayList<>())
                        .add((String) linha[1]));

        if (porCapacidade.isEmpty()) {
            return recursos;
        }

        recursos.forEach(recurso -> recurso.getPermissions().forEach(permissao -> {
            List<String> exigidas = porCapacidade.get(permissao.getActionId());
            if (exigidas != null && !exigidas.isEmpty()) {
                permissao.setRequires(exigidas.stream().sorted().toList());
            }
        }));

        return recursos;
    }

    /**
     * Agrupa as linhas por recurso e, dentro dele, por capacidade.
     *
     * <p>Uma capacidade aparece <b>uma vez</b>, com o conjunto das origens que a concederam — é o
     * que produz as etiquetas "usuário / grupo / perfil" na tela. O {@code permissionId} é o
     * primeiro não nulo entre as linhas daquela capacidade: só as vias que pertencem à entidade em
     * edição o trazem, então o resultado é a concessão removível, quando existe alguma.
     *
     * <p>A ordem de iteração é preservada ({@code LinkedHashMap}) para que a lista não mude de
     * ordem entre duas chamadas idênticas — o agrupamento anterior usava {@code HashMap} e a árvore
     * embaralhava sozinha a cada abertura do modal.
     */
    private static List<ResoucePermissionsWithTypeDto> agruparPorRecurso(List<LinhaDePermissao> linhas) {
        Map<String, List<LinhaDePermissao>> porRecurso = linhas.stream()
                .collect(Collectors.groupingBy(LinhaDePermissao::resourceId,
                        LinkedHashMap::new, Collectors.toList()));

        List<ResoucePermissionsWithTypeDto> recursos = new ArrayList<>(porRecurso.size());

        porRecurso.forEach((resourceId, doRecurso) -> {
            LinhaDePermissao primeira = doRecurso.get(0);

            Map<String, List<LinhaDePermissao>> porAcao = doRecurso.stream()
                    .collect(Collectors.groupingBy(LinhaDePermissao::actionId,
                            LinkedHashMap::new, Collectors.toList()));

            List<PermissionWithTypesDto> capacidades = new ArrayList<>(porAcao.size());

            porAcao.forEach((actionId, daAcao) -> {
                Set<SecurityType> origens = daAcao.stream()
                        .map(LinhaDePermissao::origem)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                String permissionId = daAcao.stream()
                        .map(LinhaDePermissao::permissionId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

                capacidades.add(PermissionWithTypesDto.builder()
                        .permissionId(permissionId)
                        .actionId(actionId)
                        .actionName(daAcao.get(0).actionName())
                        .actionLabel(daAcao.get(0).actionLabel())
                        .actionDescription(daAcao.get(0).actionDescription())
                        .actionCategory(daAcao.get(0).actionCategory())
                        // Vazio vira nulo para preservar a resposta anterior: a lista de
                        // disponíveis nunca teve o campo, e o DTO o omite quando nulo.
                        .types(origens.isEmpty() ? null : origens)
                        .build());
            });

            recursos.add(ResoucePermissionsWithTypeDto.builder()
                    .resourceId(resourceId)
                    .resourceName(primeira.resourceName())
                    .resourceDescription(primeira.resourceDescription())
                    .resourceType(primeira.resourceType())
                    .resourceActive(primeira.resourceActive())
                    .permissions(capacidades)
                    .build());
        });

        return recursos;
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
