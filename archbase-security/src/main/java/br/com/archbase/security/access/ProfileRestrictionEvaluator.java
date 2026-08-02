package br.com.archbase.security.access;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code @RequireProfile} — o perfil do usuário.
 *
 * <p>Ordem preservada de {@code ProfileAuthorizationManager}: a isenção de administrador vem
 * <b>antes</b> da checagem de conta ativa. É diferente de {@link RoleRestrictionEvaluator}, e essa
 * diferença é comportamento existente, não descuido a corrigir aqui.
 *
 * <p>Nota sobre {@code requireAll}: o modelo permite <b>um</b> perfil por usuário, então exigir dois
 * é insatisfazível por construção. Preservado como está — mudar seria inventar semântica nova.
 */
@Component
public class ProfileRestrictionEvaluator implements RestrictionEvaluator {

    @Override
    public RestrictionKind kind() {
        return RestrictionKind.PROFILE;
    }

    @Override
    public RestrictionResult evaluate(AccessSubject subject, Restriction restriction, AccessRequirement requirement) {
        if (restriction.allowSystemAdmin() && subject.isAdministrator() && subject.enabled()) {
            return RestrictionResult.waived("administrador isento por allowSystemAdmin");
        }

        if (restriction.requireActiveUser() && !subject.enabled()) {
            return RestrictionResult.denied(AccessReasonCodes.ACCOUNT_NOT_ACTIVE,
                    "conta desativada ou bloqueada");
        }

        if (subject.profileName() == null) {
            return RestrictionResult.denied(AccessReasonCodes.PROFILE_NOT_MATCHED,
                    "usuário sem perfil; exigido " + restriction.required());
        }

        List<String> doUsuario = List.of(subject.profileName());
        boolean tem = restriction.requireAll()
                ? doUsuario.containsAll(restriction.required())
                : restriction.required().stream().anyMatch(doUsuario::contains);

        if (!tem) {
            return RestrictionResult.denied(AccessReasonCodes.PROFILE_NOT_MATCHED,
                    "perfil " + subject.profileName() + "; exigido " + restriction.required()
                            + (restriction.requireAll() ? " (todos)" : ""));
        }

        return RestrictionResult.passed("perfil " + subject.profileName());
    }
}
