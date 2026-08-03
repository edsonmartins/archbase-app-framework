package br.com.archbase.security.controller;

import br.com.archbase.security.access.PermissionEffect;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.*;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import br.com.archbase.security.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import br.com.archbase.security.annotation.ArchbaseSecurityAdminEndpoint;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
@ArchbaseSecurityAdminEndpoint(resource = "RESOURCE")
public class ResourceController {

    private final ResourceService resourceService;
    private final ActionService actionService;
    private final UserService userService;
    private final GroupService groupService;
    private final UserProfileService userProfileService;

    @PostMapping
    public ResponseEntity<ResourceDto> createResource(@RequestBody ResourceDto resource)  {
        return ResponseEntity.ok(resourceService.createResource(resource));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceDto> updateResource(@PathVariable String id, @RequestBody ResourceDto resource)  {
        return ResponseEntity.ok(resourceService.updateResource(id, resource).get());
    }

    @DeleteMapping("/{id}")
    public void removeResoure(@PathVariable String id)  {
        resourceService.deleteResource(id);
    }

    @PostMapping("/register")
    public ResponseEntity<ResourcePermissionsDto> registerResource(@RequestBody ResourceRegisterDto resourceRegister)  {
        return ResponseEntity.ok(resourceService.registerResource(resourceRegister));
    }

    // Consulta as permissões do próprio usuário logado — autoatendimento, não administração.
    @GetMapping("/permissions/{resourceName}")
    @ArchbaseSecurityAdminEndpoint(selfService = true)
    public ResponseEntity<ResourcePermissionsDto> findLoggedUserResourcePermissions(@PathVariable String resourceName) {
        return ResponseEntity.ok(resourceService.findLoggedUserResourcePermissions(resourceName));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceDto> getResourceById(@PathVariable String id) {
        try {
            ResourceDto user = resourceService.findById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(
            value = {"/permissions/security/{id}"},
            params = {"type"}
    )
    public ResponseEntity<List<ResoucePermissionsWithTypeDto>> findResourcesPermissions(@PathVariable String id, @RequestParam("type") SecurityType type) {
        try {
            List<ResoucePermissionsWithTypeDto> resourcesPermissions = resourceService.findResourcesPermissions(id, type);
            return ResponseEntity.ok(resourcesPermissions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<ResoucePermissionsWithTypeDto>> findAllResourcesPermissions() {
        try {
            List<ResoucePermissionsWithTypeDto> resourcesPermissions = resourceService.findAllResourcesPermissions();
            return ResponseEntity.ok(resourcesPermissions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * O escopo a gravar: o pedido quando declarado, o atual quando ausente.
     *
     * <p>String vazia limpa. Sem essa distinção, "não declarei escopo" e "quero sem escopo" seriam
     * a mesma coisa — e o primeiro caso, que é o de todo cliente anterior, alargaria em silêncio
     * uma permissão que alguém estreitou de propósito.
     */
    private String escopoResolvido(String doPedido, String oAtual) {
        if (doPedido == null) {
            return oAtual;
        }
        return doPedido.isBlank() ? null : doPedido;
    }

    @PostMapping("/permissions")
    public ResponseEntity<?> grantPermission(@RequestBody GrantPermissionDto grantPermission) {
        try {
            Optional<ActionDto> action = actionService.findActionById(grantPermission.getActionId());
            if (action.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ação não encontrada");
            }
            SecurityDto security = null;
            if (grantPermission.getType().equals(SecurityType.USER)) {
                security = userService.findById(grantPermission.getSecurityId());
            }
            if (grantPermission.getType().equals(SecurityType.PROFILE)) {
                security = userProfileService.findById(grantPermission.getSecurityId());
            }
            if (grantPermission.getType().equals(SecurityType.GROUP)) {
                security = groupService.findById(grantPermission.getSecurityId());
            }

            if (security == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Entidade de segurança não encontrada");
            }

            PermissionDto existingPermission = resourceService.findPermission(security.getId(), action.get().getId());

            if (existingPermission != null) {
                // Devolver a linha existente e ignorar o pedido tornava a NEGAÇÃO inalcançável
                // exatamente no caso para o qual ela existe: "o time tem pelo perfil, tire desta
                // pessoa" pressupõe que já há uma linha para o par (segurança, ação). O endpoint
                // respondia 200 com a concessão antiga, nada era gravado, e a tela reportava
                // sucesso enquanto o acesso continuava aberto.
                PermissionEffect efeitoPedido = grantPermission.getEffect() == null
                        ? PermissionEffect.GRANT : grantPermission.getEffect();

                // Escopo AUSENTE preserva o que está gravado; só um valor explícito o substitui, e
                // string vazia é a forma de limpar. Todo cliente anterior envia apenas
                // {securityId, actionId, type}: sobrescrever com o nulo que chega alargaria uma
                // permissão estreitada a uma empresa para TODAS elas, num pedido que o operador
                // entende como "reconceder o que já estava lá".
                String empresa = escopoResolvido(grantPermission.getCompanyId(), existingPermission.getCompanyId());
                String projeto = escopoResolvido(grantPermission.getProjectId(), existingPermission.getProjectId());

                boolean mudou = efeitoPedido != existingPermission.getEffect()
                        || !java.util.Objects.equals(empresa, existingPermission.getCompanyId())
                        || !java.util.Objects.equals(projeto, existingPermission.getProjectId());

                if (!mudou) {
                    return ResponseEntity.ok(ResouceActionPermissionDto.fromPermissionDto(existingPermission));
                }

                existingPermission.setEffect(efeitoPedido);
                existingPermission.setCompanyId(empresa);
                existingPermission.setProjectId(projeto);
                PermissionDto atualizada = resourceService.grantPermission(existingPermission);
                return ResponseEntity.ok(ResouceActionPermissionDto.fromPermissionDto(atualizada));
            }

            PermissionDto permission = PermissionDto.builder()
                    .action(action.get())
                    .security(security)
                    // Nulo é GRANT. Declarar DENY aqui é o que permite tirar de uma pessoa algo
                    // que o time inteiro tem, sem criar um grupo paralelo só para excluí-la.
                    .effect(grantPermission.getEffect())
                    .companyId(grantPermission.getCompanyId())
                    .projectId(grantPermission.getProjectId())
                    .build();
            PermissionDto savedPermission = resourceService.grantPermission(permission);
            return ResponseEntity.ok(ResouceActionPermissionDto.fromPermissionDto(savedPermission));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/permissions/{id}")
    public void deletePermission(@PathVariable String id) {
        resourceService.deletePermission(id);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ResourceDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size) {
        return resourceService.findAll(page, size);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ResourceDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return resourceService.findAll(page, size, sort);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"ids"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<ResourceDto> findAll(@RequestParam(required = true) List<String> ids) {
        return resourceService.findAll(ids);
    }

    @GetMapping(
            value = {"/findWithFilter"},
            params = {"page", "size", "filter"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ResourceDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size) {
        return resourceService.findWithFilter(filter, page, size);
    }

    @GetMapping(
            value = {"/findWithFilterAndSort"},
            params = {"page", "size", "filter", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ResourceDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size, @RequestParam(value = "sort",required = true) String[] sort) {
        return resourceService.findWithFilter(filter, page, size, sort);
    }
}