package br.com.archbase.security.controller;

import br.com.archbase.security.domain.dto.ApiTokenDto;
import br.com.archbase.security.service.ApiTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tokenApi")
public class ApiTokenController {

    @Autowired
    private ApiTokenService apiTokenService;

    @PostMapping("/create")
    public ResponseEntity<ApiTokenDto> createToken(@RequestParam String email) {
        ApiTokenDto token = apiTokenService.createToken(email);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeToken(@RequestParam String token) {
        apiTokenService.revokeToken(token);
        return ResponseEntity.ok().build();
    }
}