package br.com.archbase.security.mfa;

/**
 * Resultado do início da configuração de MFA (change: MFA/2FA). {@code secret} e a
 * {@code provisioningUri} ({@code otpauth://}) são exibidos uma única vez ao usuário
 * (para digitar no app autenticador ou ler o QR Code); o segredo é persistido cifrado.
 */
public record MfaSetup(String secret, String provisioningUri) {
}
