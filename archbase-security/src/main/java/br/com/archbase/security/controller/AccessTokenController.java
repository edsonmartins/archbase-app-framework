package br.com.archbase.security.controller;

import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.AccessTokenDto;
import br.com.archbase.security.service.AccessTokenService;
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
public class AccessTokenController {

    @Autowired
    private AccessTokenService accessTokenService;


    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeToken(@RequestParam String token) {
        accessTokenService.revokeToken(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<AccessTokenDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size) {
        return accessTokenService.findAll(page, size);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<AccessTokenDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return accessTokenService.findAll(page, size, sort);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"ids"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<AccessTokenDto> findAll(@RequestParam(required = true) List<String> ids) {
        return accessTokenService.findAll(ids);
    }

    @GetMapping(
            value = {"/findWithFilter"},
            params = {"page", "size", "filter"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<AccessTokenDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size) {
        return accessTokenService.findWithFilter(filter, page, size);
    }

    @GetMapping(
            value = {"/findWithFilterAndSort"},
            params = {"page", "size", "filter", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<AccessTokenDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size, @RequestParam(value = "sort",required = true) String[] sort) {
        return accessTokenService.findWithFilter(filter, page, size, sort);
    }
}