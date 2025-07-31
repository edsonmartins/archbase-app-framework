package br.com.archbase.security.auth;

import br.com.archbase.security.service.ArchbaseAuthenticationService;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class ArchbaseAuthenticationController {

    private final ArchbaseAuthenticationService service;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
        @RequestBody AuthenticationRequest request
    ) {
        try {
            return ResponseEntity.ok(service.authenticate(request));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Endpoint de autenticação contextual com suporte a enrichers.
     * Permite que aplicações personalizem a resposta de autenticação
     * baseada no contexto (STORE_APP, CUSTOMER_APP, etc.).
     * 
     * @param contextualRequest Request com contexto da aplicação
     * @param httpRequest Request HTTP para contexto adicional
     * @return Resposta de autenticação (possivelmente enriquecida)
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginWithContext(
            @RequestBody ContextualAuthenticationRequest contextualRequest,
            HttpServletRequest httpRequest) {
        try {
            AuthenticationResponse response = service.authenticateWithContext(
                contextualRequest, 
                httpRequest
            );
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno do servidor");
        }
    }

    @PostMapping("/refresh-token")
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
