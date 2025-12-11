package br.com.archbase.security.controller;

import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.service.ActionService;
import br.com.archbase.security.service.GroupService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/action")
@Tag(name = "Ações", description = "Gestão das ações de segurança")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiTokenAuth")
public class ActionController {

    private final ActionService actionService;

    @Autowired
    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @PostMapping
    @Operation(summary = "Criar ação", description = "Cria uma nova ação de segurança")
    public ResponseEntity<ActionDto> createGroup(@RequestBody ActionDto action)  {
        return ResponseEntity.ok(actionService.createAction(action));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar ação", description = "Atualiza os dados da ação pelo ID")
    public ResponseEntity<ActionDto> updateAction(@PathVariable String id, @RequestBody ActionDto action)  {
        return ResponseEntity.ok(actionService.updateAction(id, action).get());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover ação", description = "Remove a ação correspondente ao ID informado")
    public void removeAction(@PathVariable String id)  {
        actionService.deleteAction(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ação por ID", description = "Recupera uma ação específica pelo ID")
    public ResponseEntity<ActionDto> getActionById(@PathVariable String id) {
        try {
            ActionDto user = actionService.findById(id);
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
    @Operation(summary = "Listar ações", description = "Lista ações com paginação")
    public Page<ActionDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size) {
        return actionService.findAll(page, size);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar ações com ordenação", description = "Lista ações com paginação e ordenação")
    public Page<ActionDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return actionService.findAll(page, size, sort);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"ids"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Buscar ações por IDs", description = "Busca ações pelos IDs informados")
    public List<ActionDto> findAll(@RequestParam(required = true) List<String> ids) {
        return actionService.findAll(ids);
    }

    @GetMapping(
            value = {"/findWithFilter"},
            params = {"page", "size", "filter"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar ações com filtro", description = "Lista ações aplicando filtro")
    public Page<ActionDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size) {
        return actionService.findWithFilter(filter, page, size);
    }

    @GetMapping(
            value = {"/findWithFilterAndSort"},
            params = {"page", "size", "filter", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar ações com filtro e ordenação", description = "Lista ações com filtro e ordenação")
    public Page<ActionDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size, @RequestParam(value = "sort",required = true) String[] sort) {
        return actionService.findWithFilter(filter, page, size, sort);
    }
}
