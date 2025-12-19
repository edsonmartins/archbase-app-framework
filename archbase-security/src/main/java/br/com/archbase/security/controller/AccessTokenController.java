package br.com.archbase.security.controller;

import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.AccessTokenDto;
import br.com.archbase.security.service.AccessTokenService;
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
@RequestMapping("/api/v1/accessToken")
@Tag(name = "Access Tokens", description = "Gestão de tokens de acesso gerados pelo Auth")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "apiTokenAuth")
public class AccessTokenController {

    @Autowired
    private AccessTokenService accessTokenService;


    @PostMapping("/revoke")
    @Operation(summary = "Revogar token de acesso", description = "Revoga o token de acesso informado, invalidando seu uso")
    public ResponseEntity<Void> revokeToken(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam String token) {
        accessTokenService.revokeToken(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar tokens de acesso", description = "Lista tokens de acesso com paginação")
    public Page<AccessTokenDto> findAll(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam("page") int page, @RequestParam("size") int size) {
        return accessTokenService.findAll(page, size);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar tokens de acesso com ordenação", description = "Lista tokens de acesso paginados com ordenação")
    public Page<AccessTokenDto> findAll(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return accessTokenService.findAll(page, size, sort);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"ids"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Buscar tokens de acesso por IDs", description = "Busca tokens de acesso pelos IDs informados")
    public List<AccessTokenDto> findAll(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam(required = true) List<String> ids) {
        return accessTokenService.findAll(ids);
    }

    @GetMapping(
            value = {"/findWithFilter"},
            params = {"page", "size", "filter"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar tokens de acesso com filtro", description = "Lista tokens de acesso aplicando filtro")
    public Page<AccessTokenDto> find(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size) {
        return accessTokenService.findWithFilter(filter, page, size);
    }

    @GetMapping(
            value = {"/findWithFilterAndSort"},
            params = {"page", "size", "filter", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Operation(summary = "Listar tokens de acesso com filtro e ordenação", description = "Lista tokens de acesso com filtro e ordenação")
    public Page<AccessTokenDto> find(@RequestHeader("X-TENANT-ID") String tenantId, @RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size, @RequestParam(value = "sort",required = true) String[] sort) {
        return accessTokenService.findWithFilter(filter, page, size, sort);
    }
}
