package br.com.archbase.security.controller;

import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.auth.ChangePasswordRequest;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.service.ArchbaseUserService;
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
@RequestMapping("/api/v1/user")
@Tag(name = "Usuários", description = "Gestão de usuários do sistema")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiTokenAuth")
public class UserController {

    private final ArchbaseUserService service;

    @PatchMapping
    @Operation(summary = "Alterar senha do usuário autenticado", description = "Permite o usuário logado atualizar sua própria senha")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-TENANT-ID") String tenantId,
            @RequestBody ChangePasswordRequest request,
            Principal connectedUser
    ) {
        service.changePassword(request, connectedUser);
        return ResponseEntity.ok().build();
    }


    private final UserService userService;

    @Autowired
    public UserController(ArchbaseUserService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Criar usuário", description = "Cria um novo usuário com os dados informados")
    public ResponseEntity<UserDto> createUser(@RequestHeader("X-TENANT-ID") String tenantId, @RequestBody UserDto user)  {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados do usuário informado pelo ID")
    public ResponseEntity<UserDto> updateUser(@RequestHeader("X-TENANT-ID") String tenantId, @PathVariable String id, @RequestBody UserDto user)  {
        return ResponseEntity.ok(userService.updateUser(id, user).get());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover usuário", description = "Remove o usuário correspondente ao ID informado")
    public void removeUser(@RequestHeader("X-TENANT-ID") String tenantId, @PathVariable String id)  {
        userService.removeUser(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Recupera os detalhes do usuário a partir do seu ID")
    public ResponseEntity<UserDto> getUserById(@RequestHeader("X-TENANT-ID") String tenantId, @PathVariable String id) {
        try {
            UserDto user = userService.findById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/byEmail/{email}")
    @Operation(summary = "Buscar usuário por email", description = "Recupera um usuário pelo endereço de email")
    public ResponseEntity<UserDto> getUserByEmail(@RequestHeader("X-TENANT-ID") String tenantId, @PathVariable String email) {
        try {
            Optional<User> userOptional = userService.getUserByEmail(email);
            return userOptional.map(user -> ResponseEntity.ok(UserDto.fromDomain(user))).orElseGet(() -> ResponseEntity.notFound().build());
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
    @Operation(summary = "Listar usuários", description = "Lista usuários com paginação")
    public Page<UserDto> findAll(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam("page") int page, @RequestParam("size") int size) {
        return userService.findAll(page, size);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar usuários com ordenação", description = "Lista usuários com paginação e ordenação")
    public Page<UserDto> findAll(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return userService.findAll(page, size, sort);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"ids"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Buscar usuários por IDs", description = "Busca usuários pelos IDs informados")
    public List<UserDto> findAll(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam(required = true) List<String> ids) {
        return userService.findAll(ids);
    }

    @GetMapping(
            value = {"/findWithFilter"},
            params = {"page", "size", "filter"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar usuários com filtro", description = "Lista usuários aplicando filtro")
    public Page<UserDto> find(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size) {
        return userService.findWithFilter(filter, page, size);
    }

    @GetMapping(
            value = {"/findWithFilterAndSort"},
            params = {"page", "size", "filter", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar usuários com filtro e ordenação", description = "Lista usuários com filtro e ordenação")
    public Page<UserDto> find(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size, @RequestParam(value = "sort",required = true) String[] sort) {
        return userService.findWithFilter(filter, page, size, sort);
    }
}
