package br.com.archbase.security.token;

/**
 * Para que serve a linha em {@code SEGURANCA_TOKEN_ACESSO}.
 *
 * <p>Access e refresh convivem na mesma tabela porque compartilham ciclo de vida: revogar a sessão
 * precisa derrubar os dois de uma vez. O que não pode é confundi-los na hora de autenticar — daí
 * esta coluna, que separa "credencial de acesso" de "autorização para renovar".
 *
 * <p>Linha com {@code null} é anterior a esta versão e vale como {@link #ACCESS}: até então só
 * access token era persistido.
 */
public enum TokenUse {
    ACCESS,
    REFRESH
}
