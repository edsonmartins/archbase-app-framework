package br.com.archbase.security.auth;

import br.com.archbase.security.service.ArchbaseAuthenticationService;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Autenticação e gestão de credenciais")
public class ArchbaseAuthenticationController {

    private final ArchbaseAuthenticationService service;

    @PostMapping("/authenticate")
    @Operation(summary = "Autenticar usuário", description = "Autentica com email e senha")
    public ResponseEntity<?> authenticate(
        @RequestBody AuthenticationRequest request
    ) {
        try {
            return ResponseEntity.ok(service.authenticate(request));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Renovar access token", description = "Gera novo access token a partir de um refresh token válido")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshToken) {
        try {
            return ResponseEntity.ok(service.refreshToken(refreshToken));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (ArchbaseValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/sendResetPasswordEmail/{email}")
    @Operation(summary = "Enviar email de redefinição de senha")
    public ResponseEntity<?> sendResetPasswordEmail(@PathVariable String email) {
        try {
            service.sendResetPasswordEmail(email);
            return ResponseEntity.ok().build();
        } catch (ArchbaseValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/resetPassword")
    @Operation(summary = "Redefinir senha")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            service.resetPassword(request);
            return ResponseEntity.ok().build();
        } catch (ArchbaseValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
