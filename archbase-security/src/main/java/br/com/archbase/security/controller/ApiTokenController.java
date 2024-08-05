package br.com.archbase.security.controller;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.domain.dto.ApiTokenDto;
import br.com.archbase.security.service.ApiTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/apiToken")
public class ApiTokenController {

    private static final Logger logger = LoggerFactory.getLogger(ApiTokenController.class);


    @Autowired
    private ApiTokenService apiTokenService;

    @PostMapping("/create")
    public ResponseEntity<ApiTokenDto> createToken(@RequestParam String email, @RequestParam LocalDateTime expirationDate, @RequestParam String name, @RequestParam String description) {
        ApiTokenDto token = apiTokenService.createToken(email,expirationDate, name, description);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeToken(@RequestParam String token) {
        apiTokenService.revokeToken(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activateToken(@RequestParam String token, @RequestParam String tenantId) {
        logger.info("Recebida solicitação para ativar token: {} com tenantId: {}", token, tenantId);
        // Set tenantId in context
        ArchbaseTenantContext.setTenantId(tenantId);

        boolean activated = apiTokenService.activateToken(token, tenantId);
        if (activated) {
            logger.info("Token ativado com sucesso: {}", token);
            return ResponseEntity.ok(generateHtmlResponse("Token ativado com sucesso.", true));
        } else {
            logger.warn("Falha ao ativar token: {}. Token inválido ou já ativado.", token);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(generateHtmlResponse("Token inválido ou já ativado.", false));
        }
    }

    private String generateHtmlResponse(String message, boolean success) {
        return "<html>" +
                "<head>" +
                "<title>Ativação de Token</title>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: " + (success ? "#e0ffe0" : "#ffe0e0") + "; }" +
                ".container { max-width: 600px; margin: 50px auto; padding: 20px; border-radius: 10px; background-color: #fff; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); }" +
                "h1 { color: " + (success ? "#4CAF50" : "#F44336") + "; }" +
                "p { font-size: 18px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<h1>" + (success ? "Sucesso!" : "Erro!") + "</h1>" +
                "<p>" + message + "</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ApiTokenDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size) {
        return apiTokenService.findAll(page, size);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ApiTokenDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return apiTokenService.findAll(page, size, sort);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"ids"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<ApiTokenDto> findAll(@RequestParam(required = true) List<String> ids) {
        return apiTokenService.findAll(ids);
    }

    @GetMapping(
            value = {"/findWithFilter"},
            params = {"page", "size", "filter"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ApiTokenDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size) {
        return apiTokenService.findWithFilter(filter, page, size);
    }

    @GetMapping(
            value = {"/findWithFilterAndSort"},
            params = {"page", "size", "filter", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ApiTokenDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size, @RequestParam(value = "sort",required = true) String[] sort) {
        return apiTokenService.findWithFilter(filter, page, size, sort);
    }
}