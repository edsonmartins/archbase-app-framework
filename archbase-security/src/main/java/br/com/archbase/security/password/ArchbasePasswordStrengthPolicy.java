package br.com.archbase.security.password;

import br.com.archbase.validation.exception.ArchbaseValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Regras de força de senha, aplicadas em todo caminho que grava uma senha.
 *
 * <p><b>Por que existe.</b> {@code ArchbasePasswordPolicy} só trata <i>expiração</i>: quando a senha
 * vence. Nada no framework olhava o <i>conteúdo</i> — {@code resetPassword} aceitava {@code "1"} e
 * gravava o bcrypt tranquilamente. Expirar a cada 90 dias uma senha de um caractere não protege
 * ninguém.
 *
 * <p><b>Desligada por padrão</b> ({@code min-length=0}): ligar regras de senha numa base existente
 * não invalida as senhas já gravadas, mas passa a recusar trocas que antes passavam — é uma decisão
 * de produto, com impacto em suporte, e não algo que uma atualização de framework deva impor.
 * Habilite com:
 *
 * <pre>
 * archbase.security.password.min-length=12
 * archbase.security.password.require-digit=true
 * archbase.security.password.require-uppercase=true
 * archbase.security.password.require-lowercase=true
 * archbase.security.password.require-special=true
 * </pre>
 */
@Component
@Slf4j
public class ArchbasePasswordStrengthPolicy {

    /**
     * Senhas notoriamente comuns. Lista curta de propósito: cobre o que aparece em ataque de
     * dicionário básico sem virar um recurso a ser mantido dentro do framework. Quem precisa de
     * verificação séria deve integrar uma fonte externa (HIBP, por exemplo).
     */
    private static final Set<String> COMUNS = Set.of(
            "123456", "1234567", "12345678", "123456789", "1234567890",
            "password", "senha", "qwerty", "abc123", "111111", "000000",
            "admin", "administrador", "welcome", "iloveyou", "123123");

    /** {@code 0} desliga toda a validação de força. */
    @Value("${archbase.security.password.min-length:0}")
    private int minLength;

    @Value("${archbase.security.password.require-digit:false}")
    private boolean requireDigit;

    @Value("${archbase.security.password.require-uppercase:false}")
    private boolean requireUppercase;

    @Value("${archbase.security.password.require-lowercase:false}")
    private boolean requireLowercase;

    @Value("${archbase.security.password.require-special:false}")
    private boolean requireSpecial;

    /** Recusa as senhas da lista {@link #COMUNS}. Vale mesmo com as demais regras desligadas. */
    @Value("${archbase.security.password.block-common:true}")
    private boolean blockCommon;

    public boolean isEnabled() {
        return minLength > 0 || requireDigit || requireUppercase || requireLowercase
                || requireSpecial || blockCommon;
    }

    /**
     * @throws ArchbaseValidationException se a senha não atende às regras, com a lista completa do
     *         que falta — informar um requisito por vez faz o usuário tentar N vezes
     */
    public void validate(String password) {
        if (!isEnabled()) {
            return;
        }
        if (password == null || password.isEmpty()) {
            throw new ArchbaseValidationException("A senha não pode ser vazia.");
        }

        List<String> problemas = new ArrayList<>();

        if (minLength > 0 && password.length() < minLength) {
            problemas.add(String.format("ter no mínimo %d caracteres", minLength));
        }
        if (requireDigit && password.chars().noneMatch(Character::isDigit)) {
            problemas.add("conter ao menos um número");
        }
        if (requireUppercase && password.chars().noneMatch(Character::isUpperCase)) {
            problemas.add("conter ao menos uma letra maiúscula");
        }
        if (requireLowercase && password.chars().noneMatch(Character::isLowerCase)) {
            problemas.add("conter ao menos uma letra minúscula");
        }
        if (requireSpecial && password.chars().allMatch(Character::isLetterOrDigit)) {
            problemas.add("conter ao menos um caractere especial");
        }
        if (blockCommon && COMUNS.contains(password.toLowerCase())) {
            problemas.add("não ser uma senha de uso comum");
        }

        if (!problemas.isEmpty()) {
            throw new ArchbaseValidationException("A senha deve " + String.join(", ", problemas) + ".");
        }
    }
}
