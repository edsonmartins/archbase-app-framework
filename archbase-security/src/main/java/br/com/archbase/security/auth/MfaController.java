package br.com.archbase.security.auth;

import br.com.archbase.security.mfa.MfaService;
import br.com.archbase.security.mfa.MfaSetup;
import br.com.archbase.security.persistence.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Configuração do segundo fator (MFA/TOTP) do <b>usuário autenticado</b>. Consumidores devem
 * proteger {@code /api/v1/mfa/**} (exige autenticação). O passo 2 do login (verificação com o
 * token de desafio) fica em {@code POST /api/v1/auth/mfa/verify} (público).
 */
@RestController
@RequestMapping("/api/v1/mfa")
@RequiredArgsConstructor
@Tag(name = "MFA", description = "Configuração do segundo fator (TOTP) do usuário autenticado")
public class MfaController {

    private final MfaService mfaService;

    @Value("${archbase.security.mfa.issuer:Archbase}")
    private String issuer;

    @GetMapping("/status")
    @Operation(summary = "Indica se o MFA está habilitado para o usuário autenticado")
    public ResponseEntity<?> status(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("enabled", mfaService.isMfaEnabled(user)));
    }

    @PostMapping("/setup")
    @Operation(summary = "Inicia a configuração: gera o segredo e a URI do QR Code (MFA ainda não ativo)")
    public ResponseEntity<MfaSetup> setup(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(mfaService.iniciarConfiguracao(user.getId(), issuer));
    }

    @PostMapping("/enable")
    @Operation(summary = "Confirma o primeiro código TOTP, ativa o MFA e devolve os códigos de recuperação")
    public ResponseEntity<?> enable(@AuthenticationPrincipal UserEntity user,
                                    @RequestBody MfaEnableRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            List<String> recoveryCodes = mfaService.ativar(user.getId(), request.code());
            return ResponseEntity.ok(Map.of("recoveryCodes", recoveryCodes));
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Map.of recusa valor nulo: uma exceção sem mensagem faria o próprio catch estourar
            // com NPE, trocando o 422 pretendido por um 500 sem corpo.
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("message", e.getMessage() != null
                            ? e.getMessage()
                            : "Não foi possível ativar o segundo fator."));
        }
    }

    @PostMapping("/disable")
    @Operation(summary = "Desativa o MFA e apaga o segredo/códigos de recuperação")
    public ResponseEntity<?> disable(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        mfaService.desativar(user.getId());
        return ResponseEntity.noContent().build();
    }
}
