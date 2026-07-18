package br.com.archbase.security.auth;

/**
 * Corpo do passo 2 do login com MFA ({@code POST /auth/mfa/verify}): o token de desafio
 * emitido no passo da senha + o código do segundo fator (TOTP ou código de recuperação).
 */
public record MfaVerifyRequest(String challengeToken, String code) {
}
