package br.com.archbase.security.service;

import br.com.archbase.ddd.domain.base.ArchbaseIdentifier;
import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.security.adapter.ActionPersistenceAdapter;
import br.com.archbase.security.adapter.SecurityAdapter;
import br.com.archbase.security.domain.dto.*;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.domain.entity.User;
import com.google.common.collect.Lists;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import br.com.archbase.security.adapter.ResourcePersistenceAdapter;
import br.com.archbase.security.usecase.ResourceUseCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ResourceService implements ResourceUseCase, FindDataWithFilterQuery<String, ResourceDto> {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ResourceService.class);

    private final ResourcePersistenceAdapter adapter;
    private final SecurityAdapter securityAdapter;
    private final ActionPersistenceAdapter actionPersistenceAdapter;
    private final ArchbaseCapabilityDependencyService dependencyService;

    @Autowired
    public ResourceService(ResourcePersistenceAdapter adapter, SecurityAdapter securityAdapter,
                           ActionPersistenceAdapter actionPersistenceAdapter,
                           ArchbaseCapabilityDependencyService dependencyService) {
        this.adapter = adapter;
        this.securityAdapter = securityAdapter;
        this.actionPersistenceAdapter = actionPersistenceAdapter;
        this.dependencyService = dependencyService;
    }

    @Override
    public List<ResourceDto> findAllResources() {
        return adapter.findAllResources();
    }

    @Override
    public Optional<ResourceDto> findResourceById(String id) {
        return adapter.findResourceById(id);
    }

    @Override
    public ResourceDto createResource(ResourceDto resourceDto) {
        return adapter.createResource(resourceDto);
    }

    @Override
    public Optional<ResourceDto> updateResource(String id, ResourceDto resourceDto) {
        return adapter.updateResource(id, resourceDto);
    }

    @Override
    public void deleteResource(String id) {
        adapter.deleteResource(id);
    }

    @Override
    @Transactional
    public ResourcePermissionsDto registerResource(ResourceRegisterDto resourceRegister) {
        ResourceDto resourceDto = adapter.findResource(resourceRegister.getResource().getResourceName());
        if (resourceDto == null) {
            resourceDto = ResourceDto.builder()
                    .id(new ArchbaseIdentifier().toString())
                    .name(resourceRegister.getResource().getResourceName())
                    .description(resourceRegister.getResource().getResourceDescription())
                    .createEntityDate(LocalDateTime.now())
                    .updateEntityDate(LocalDateTime.now())
                    .createdByUser("archbase")
                    .version(0L)
                    .active(true)
                    .type(TipoRecurso.VIEW)
                    .build();

            resourceDto = adapter.createResource(resourceDto);
        } else if (resourceDto.getType() == null) {
            // CLASSIFICACAO RETROATIVA, e so isso — o espelho do que a varredura faz com API.
            //
            // TIPO_RECURSO e nulavel e chegou depois de muita gente ja ter catalogo. A tela de
            // permissoes passa a separar recurso de tela de recurso de endpoint por este campo, e
            // sem preencher o que ja existe a separacao nasceria vazia justamente nas bases que
            // mais precisam dela.
            //
            // So preenche o NULO: recurso que ja tem tipo nao e tocado, para que a classificacao
            // nao oscile entre a subida da aplicacao e a abertura da tela.
            resourceDto = adapter.classificarSeSemTipo(resourceDto.getId(), TipoRecurso.VIEW)
                    .orElse(resourceDto);
        }
        final var finalResourceDto = resourceDto;
        // O REGISTRO DE TELA É ADITIVO: cria o que falta, reativa o que voltou, e NUNCA desativa.
        //
        // Antes, toda ação do recurso ausente do payload era desativada. A regra parece razoável
        // — "sumiu do código, sai do catálogo" — e é insustentável aqui, por duas razões que se
        // somam:
        //
        // 1. QUEM REGISTRA CONHECE UMA PARTE. Um recurso pode ser declarado por mais de uma tela,
        //    e cada uma envia só as ações que ela usa. Com desativação, duas telas do mesmo
        //    recurso apagam as ações uma da outra a cada abertura, alternadamente. Não há payload
        //    "completo" para comparar contra: o cliente não tem essa informação.
        //
        // 2. PAYLOAD VAZIO É O CASO EXTREMO DISSO. O ArchbaseViewSecurityProvider do archbase-react
        //    renderizava o loading no lugar dos filhos e chamava apply() antes de qualquer
        //    registerAction, então a lista chegava vazia — e "vazia" casava com "todas faltando".
        //    A primeira abertura de cada tela zerava o catálogo do recurso. No gestor-rq isso
        //    zerou 56 dos 100 recursos e deixou 1.333 das 2.131 concessões (63%) apontando para
        //    ação inativa: invisíveis na tela e prestes a virar revogação em massa no dia em que
        //    permission.require-active fosse ligado.
        //
        // Poda continua existindo onde ela é sólida: no catálogo de tipo API, que o
        // ArchbaseActionSynchronizationService varre a partir do código — lá existe a lista
        // completa, e existe o sync.mode=report para conferir antes de escrever. Aqui não existe
        // nem uma coisa nem outra, e o preço do erro é acesso perdido em silêncio.
        //
        // Consequência aceita: ação renomeada permanece ativa no catálogo até alguém desativá-la
        // pelo admin. Fica visível e concedível sem efeito — barulhento e reversível, ao contrário
        // do que se perdia antes.

        resourceRegister.getActions().forEach(simpleActionDto -> {
            Optional<ActionDto> actionOptional = actionPersistenceAdapter
                    .findActionByName(simpleActionDto.getActionName(), finalResourceDto.getId());
            ActionDto capacidade;
            if (actionOptional.isEmpty()) {
                ActionDto action = ActionDto.builder()
                        .name(simpleActionDto.getActionName())
                        .description(simpleActionDto.getActionDescription())
                        .label(simpleActionDto.getActionLabel())
                        .category(simpleActionDto.getActionCategory())
                        .resource(finalResourceDto)
                        .createEntityDate(LocalDateTime.now())
                        .updateEntityDate(LocalDateTime.now())
                        .createdByUser("archbase")
                        .version(0L)
                        .active(true)
                        .build();
                capacidade = actionPersistenceAdapter.createAction(action);
            } else {
                ActionDto foundAction = actionOptional.get();
                if (!foundAction.getActive()) {
                    foundAction.setActive(true);
                    actionPersistenceAdapter.updateAction(foundAction.getId(), foundAction);
                }
                capacidade = foundAction;
                // Semeia o que nunca foi semeado. Rótulo e categoria são campos novos, e nulo numa
                // capacidade existente é ausência do campo na versão em que a linha nasceu — não
                // decisão de quem administra.
                actionPersistenceAdapter.semearTextosSeAusentes(foundAction.getId(),
                        simpleActionDto.getActionLabel(), simpleActionDto.getActionCategory());
            }
            sincronizarDependencias(simpleActionDto, capacidade, finalResourceDto);
        });
        return adapter.findLoggedUserResourcePermissions(finalResourceDto.getName());
    }

    /**
     * Grava as dependências que a tela declarou para esta capacidade.
     *
     * <p><b>Escopada à ação, e só às ações do payload.</b> Um recurso pode ser declarado por mais de
     * uma tela, e cada uma envia só as ações que usa — a mesma razão que tornou o registro aditivo.
     * Ação ausente do payload não chega aqui, e portanto não é tocada.
     *
     * <p>{@code requires} nulo — o que todo cliente anterior envia — significa "não declarei" e sai
     * sem fazer nada. Lista vazia significa "não há nenhuma" e remove as que existirem.
     */
    private void sincronizarDependencias(SimpleActionDto declarada, ActionDto capacidade,
                                         ResourceDto recurso) {
        if (declarada.getRequires() == null || capacidade == null || capacidade.getId() == null) {
            return;
        }

        ArchbaseCapabilityDependencyService.Qualificadas qualificadas =
                ArchbaseCapabilityDependencyService.qualificar(
                        declarada.getRequires(), recurso.getName(), capacidade.getName());

        // Aviso, e não erro: isto é entrada de cliente, e o resto do registro segue valendo.
        qualificadas.invalidas().forEach(invalida -> log.warn(
                "Registro da tela '{}' declara requires=\"{}\" na ação '{}', que não é uma capacidade "
                        + "válida. Use \"acao\" para o mesmo recurso ou \"recurso:acao\" para outro. "
                        + "Ignorada.",
                recurso.getName(), invalida, capacidade.getName()));

        qualificadas.autoReferencias().forEach(auto -> log.warn(
                "Registro da tela '{}' declara dependência de si mesma ('{}'). Ignorada.",
                recurso.getName(), auto));

        dependencyService.reconcileRegisterById(capacidade.getId(), qualificadas.validas());
    }

    /**
     * Tudo de que uma capacidade depende, direta e indiretamente.
     *
     * <p>Vive aqui, e não em {@code ResourceUseCase}, porque acrescentar método a uma interface
     * pública do framework quebra quem a implementa fora deste repositório. O controller já depende
     * da classe concreta.
     */
    public Optional<CapabilityDependencyTreeDto> findCapabilityDependencies(String actionId) {
        return dependencyService.closureOf(actionId);
    }

    @Override
    public ResourcePermissionsDto findLoggedUserResourcePermissions(String resourceName) {
        return adapter.findLoggedUserResourcePermissions(resourceName);
    }

    @Override
    public LoggedUserPermissionsDto findLoggedUserPermissions() {
        return adapter.findLoggedUserPermissions();
    }

    public List<ResoucePermissionsWithTypeDto> findResourcesPermissions(String securityId, SecurityType securityType) {
        if (SecurityType.USER.equals(securityType)) {
            return adapter.findUserResourcesPermissions(securityId);
        }
        if (SecurityType.PROFILE.equals(securityType)) {
            return adapter.findProfileResourcesPermissions(securityId);
        }
        if (SecurityType.GROUP.equals(securityType)) {
            return adapter.findGroupResourcesPermissions(securityId);
        }
        return Lists.newArrayList();
    }

    public List<ResoucePermissionsWithTypeDto> findAllResourcesPermissions() {
        return adapter.findAllResourcesPermissions();
    }

    public PermissionDto findPermission(String securityId, String actionId) {
        return adapter.findPermission(securityId, actionId);
    }

    public PermissionDto grantPermission(PermissionDto permissionDto) {
        User user = securityAdapter.getLoggedUser();
        permissionDto.setCreatedByUser(user.getUserName());
        permissionDto.setLastModifiedByUser(user.getUserName());
        permissionDto.setUpdateEntityDate(LocalDateTime.now());
        permissionDto.setCreateEntityDate(LocalDateTime.now());
        return adapter.grantPermission(permissionDto);
    }

    public void deletePermission(String id) {
        adapter.deletePermission(id);
    }

    @Override
    public ResourceDto findById(String s) {
        return adapter.findById(s);
    }

    @Override
    public Page<ResourceDto> findAll(int page, int size) {
        return adapter.findAll(page,size);
    }

    @Override
    public Page<ResourceDto> findAll(int page, int size, String[] sort) {
        return adapter.findAll(page,size,sort);
    }

    @Override
    public List<ResourceDto> findAll(List<String> strings) {
        return adapter.findAll(strings);
    }

    @Override
    public Page<ResourceDto> findWithFilter(String filter, int page, int size) {
        return adapter.findWithFilter(filter,page,size);
    }

    @Override
    public Page<ResourceDto> findWithFilter(String filter, int page, int size, String[] sort) {
        return adapter.findWithFilter(filter,page,size,sort);
    }
}
