package br.com.archbase.security.spi;

import br.com.archbase.security.persistence.UserEntity;

import java.util.List;

/**
 * Decide se um usuário pode usar <b>senha local</b> do Archbase: entrar com ela, trocá-la e redefini-la.
 * <p>
 * Existe para usuários cuja credencial pertence a outro sistema — por exemplo, um usuário vinculado a um IdP externo
 * (módulo {@code archbase-security-oidc}) cuja aplicação optou por desligar a senha local ao vincular. Nesse caso a
 * senha é do IdP; aceitar, trocar ou redefinir uma senha local paralela criaria uma porta de entrada que o IdP não
 * controla (nem MFA, nem bloqueio, nem desligamento).
 * <p>
 * <b>Sem nenhum bean deste tipo, nada muda:</b> todo usuário pode usar senha local, exatamente como antes. Com
 * vários, a senha local só vale se <b>todos</b> permitirem.
 * <p>
 * Onde é consultada, e o que o cliente vê quando nega:
 * <ul>
 * <li>login por senha — a mesma resposta de senha errada, para não revelar que a conta é federada;</li>
 * <li>troca da própria senha — recusada;</li>
 * <li>pedido de e-mail de redefinição — tratado como o usuário com {@code allowPasswordChange=false}, inclusive na
 * resposta uniforme de {@code archbase.security.prevent-user-enumeration};</li>
 * <li>conclusão da redefinição — recusada.</li>
 * </ul>
 *
 * @since 3.5.0
 */
public interface ArchbaseLocalCredentialPolicy {

    /** Se {@code user} pode autenticar, trocar e redefinir senha local do Archbase. */
    boolean allowsLocalPassword(UserEntity user);

    /**
     * Combina as políticas registradas: {@code true} se não houver nenhuma, ou se todas permitirem. Uma política que
     * lança exceção conta como negação — falha ao decidir não pode abrir a senha local.
     */
    static boolean allowsLocalPassword(List<ArchbaseLocalCredentialPolicy> policies, UserEntity user) {
        if (policies == null || policies.isEmpty()) {
            return true;
        }
        for (ArchbaseLocalCredentialPolicy policy : policies) {
            try {
                if (!policy.allowsLocalPassword(user)) {
                    return false;
                }
            } catch (RuntimeException e) {
                return false;
            }
        }
        return true;
    }
}
