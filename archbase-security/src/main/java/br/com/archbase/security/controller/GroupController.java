package br.com.archbase.security.controller;

import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.auth.ChangePasswordRequest;
import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.Group;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.service.ArchbaseUserService;
import br.com.archbase.security.service.GroupService;
import br.com.archbase.security.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/group")
@Tag(name = "Grupos", description = "Gestão de grupos de usuários")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiTokenAuth")
public class GroupController {

    private final GroupService groupService;

    @Autowired
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @Operation(summary = "Criar grupo", description = "Cria um novo grupo de usuários")
    public ResponseEntity<GroupDto> createGroup(@RequestHeader("X-TENANT-ID") String tenantId, @RequestBody GroupDto group)  {
        return ResponseEntity.ok(groupService.createGroup(group));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar grupo", description = "Atualiza os dados do grupo identificado pelo ID")
    public ResponseEntity<GroupDto> updateGroup(@RequestHeader("X-TENANT-ID") String tenantId, @PathVariable String id, @RequestBody GroupDto group)  {
        return ResponseEntity.ok(groupService.updateGroup(id, group).get());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover grupo", description = "Remove o grupo correspondente ao ID informado")
    public void removeGroup(@RequestHeader("X-TENANT-ID") String tenantId, @PathVariable String id)  {
        groupService.deleteGroup(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar grupo por ID", description = "Recupera os detalhes do grupo pelo ID")
    public ResponseEntity<GroupDto> getGroupById(@RequestHeader("X-TENANT-ID") String tenantId, @PathVariable String id) {
        try {
            GroupDto user = groupService.findById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar grupos", description = "Lista grupos com paginação")
    public Page<GroupDto> findAll(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam("page") int page, @RequestParam("size") int size) {
        return groupService.findAll(page, size);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar grupos com ordenação", description = "Lista grupos com paginação e ordenação")
    public Page<GroupDto> findAll(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return groupService.findAll(page, size, sort);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"ids"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Buscar grupos por IDs", description = "Busca grupos pelos IDs informados")
    public List<GroupDto> findAll(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam(required = true) List<String> ids) {
        return groupService.findAll(ids);
    }

    @GetMapping(
            value = {"/findWithFilter"},
            params = {"page", "size", "filter"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar grupos com filtro", description = "Lista grupos aplicando filtro")
    public Page<GroupDto> find(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size) {
        return groupService.findWithFilter(filter, page, size);
    }

    @GetMapping(
            value = {"/findWithFilterAndSort"},
            params = {"page", "size", "filter", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar grupos com filtro e ordenação", description = "Lista grupos com filtro e ordenação")
    public Page<GroupDto> find(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size, @RequestParam(value = "sort",required = true) String[] sort) {
        return groupService.findWithFilter(filter, page, size, sort);
    }
}
