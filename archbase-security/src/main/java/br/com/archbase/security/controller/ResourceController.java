package br.com.archbase.security.controller;

import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.*;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import br.com.archbase.security.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resource")
@Tag(name = "Recursos e Permissões", description = "Gestão de recursos e permissões do sistema")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiTokenAuth")
public class ResourceController {

    private final ResourceService resourceService;
    private final ActionService actionService;
    private final UserService userService;
    private final GroupService groupService;
    private final UserProfileService userProfileService;

    @PostMapping
    @Operation(summary = "Criar recurso", description = "Cria um novo recurso de segurança")
    public ResponseEntity<ResourceDto> createResource(@RequestBody ResourceDto resource)  {
        return ResponseEntity.ok(resourceService.createResource(resource));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar recurso", description = "Atualiza os dados do recurso identificado pelo ID")
    public ResponseEntity<ResourceDto> updateResource(@PathVariable String id, @RequestBody ResourceDto resource)  {
        return ResponseEntity.ok(resourceService.updateResource(id, resource).get());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover recurso", description = "Remove o recurso correspondente ao ID informado")
    public void removeResoure(@PathVariable String id)  {
        resourceService.deleteResource(id);
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar recurso e permissões iniciais", description = "Registra o recurso e cria permissões padrão para ele")
    public ResponseEntity<ResourcePermissionsDto> registerResource(@RequestBody ResourceRegisterDto resourceRegister)  {
        return ResponseEntity.ok(resourceService.registerResource(resourceRegister));
    }

    @GetMapping("/permissions/{resourceName}")
    @Operation(summary = "Listar permissões do recurso para usuário logado", description = "Retorna as permissões do recurso para o usuário autenticado")
    public ResponseEntity<ResourcePermissionsDto> findLoggedUserResourcePermissions(@PathVariable String resourceName) {
        return ResponseEntity.ok(resourceService.findLoggedUserResourcePermissions(resourceName));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar recurso por ID", description = "Recupera os detalhes do recurso pelo ID")
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
    @Operation(summary = "Listar permissões por entidade de segurança (usuário, perfil ou grupo)", description = "Lista as permissões do recurso considerando o tipo de entidade de segurança informado")
    public ResponseEntity<List<ResoucePermissionsWithTypeDto>> findResourcesPermissions(@PathVariable String id, @RequestParam("type") SecurityType type) {
        try {
            List<ResoucePermissionsWithTypeDto> resourcesPermissions = resourceService.findResourcesPermissions(id, type);
            return ResponseEntity.ok(resourcesPermissions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/permissions")
    @Operation(summary = "Listar permissões de todos os recursos", description = "Lista todas as permissões existentes para cada recurso")
    public ResponseEntity<List<ResoucePermissionsWithTypeDto>> findAllResourcesPermissions() {
        try {
            List<ResoucePermissionsWithTypeDto> resourcesPermissions = resourceService.findAllResourcesPermissions();
            return ResponseEntity.ok(resourcesPermissions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/permissions")
    @Operation(summary = "Conceder permissão a usuário, perfil ou grupo", description = "Concede uma ação a uma entidade de segurança, criando a permissão caso não exista")
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
                return ResponseEntity.ok(ResouceActionPermissionDto.fromPermissionDto(existingPermission));
            }

            PermissionDto permission = PermissionDto.builder()
                    .action(action.get())
                    .security(security)
                    .build();
            PermissionDto savedPermission = resourceService.grantPermission(permission);
            return ResponseEntity.ok(ResouceActionPermissionDto.fromPermissionDto(savedPermission));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/permissions/{id}")
    @Operation(summary = "Remover permissão", description = "Remove a permissão correspondente ao ID informado")
    public void deletePermission(@PathVariable String id) {
        resourceService.deletePermission(id);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar recursos", description = "Lista recursos com paginação")
    public Page<ResourceDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size) {
        return resourceService.findAll(page, size);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar recursos com ordenação", description = "Lista recursos com paginação e ordenação")
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
    @Operation(summary = "Buscar recursos por IDs", description = "Busca recursos pelos IDs informados")
    public List<ResourceDto> findAll(@RequestParam(required = true) List<String> ids) {
        return resourceService.findAll(ids);
    }

    @GetMapping(
            value = {"/findWithFilter"},
            params = {"page", "size", "filter"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar recursos com filtro", description = "Lista recursos aplicando filtro")
    public Page<ResourceDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size) {
        return resourceService.findWithFilter(filter, page, size);
    }

    @GetMapping(
            value = {"/findWithFilterAndSort"},
            params = {"page", "size", "filter", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar recursos com filtro e ordenação", description = "Lista recursos com filtro e ordenação")
    public Page<ResourceDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size, @RequestParam(value = "sort",required = true) String[] sort) {
        return resourceService.findWithFilter(filter, page, size, sort);
    }
}
