package br.com.archbase.security.controller;

import br.com.archbase.security.domain.dto.SimpleUserDto;
import br.com.archbase.security.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simple-user")
@Tag(name = "Usuários Simples", description = "Operações simplificadas de gestão de usuários")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiTokenAuth")
public class SimpleUserController {

    private final UserService userService;

    @Autowired
    public SimpleUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Criar usuário simples", description = "Cria um novo usuário a partir de dados simplificados (SimpleUserDto)")
    public ResponseEntity<String> createUser(
            @RequestHeader("X-TENANT-ID") String tenantId,
            @RequestBody SimpleUserDto user) {
        return ResponseEntity.ok(userService.createSimpleUser(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário simples", description = "Atualiza um usuário existente usando dados simplificados (SimpleUserDto)")
    public ResponseEntity<String> updateUser(
            @RequestHeader("X-TENANT-ID") String tenantId,
            @PathVariable String id,
            @RequestBody SimpleUserDto user) {
        return ResponseEntity.ok(userService.updateSimpleUser(id, user));
    }
}
