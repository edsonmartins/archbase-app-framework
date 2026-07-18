package br.com.archbase.security.auth;

/** Corpo da ativação de MFA ({@code POST /mfa/enable}): o primeiro código TOTP a confirmar. */
public record MfaEnableRequest(String code) {
}
