package br.com.archbase.security.access;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code @RequirePersona} — personas de negócio.
 *
 * <p><b>Aviso sobre o mapeamento.</b> A tabela abaixo veio de
 * {@code PersonaAuthorizationManager.validateBasicPersonaMapping} e traz vocabulário de outro
 * domínio embutido no framework: {@code PLATFORM_ADMIN}, {@code STORE_ADMIN}, {@code CUSTOMER},
 * {@code DRIVER}. Um perfil chamado {@code ADMIN} casa com a persona {@code PLATFORM_ADMIN} por
 * essa tabela — não por configuração do cliente, o que contraria o princípio de que vocabulário é
 * do cliente.
 *
 * <p>Está preservada aqui <b>tal e qual</b>, porque removê-la mudaria decisão em aplicações que
 * dependem dela. A substituição por resolução configurável é item do plano do core, não deste
 * passo. Ver {@code MODELO_CORE_AUTORIZACAO.md}.
 */
@Component
public class PersonaRestrictionEvaluator implements RestrictionEvaluator {

    @Override
    public RestrictionKind kind() {
        return RestrictionKind.PERSONA;
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
            return RestrictionResult.denied(AccessReasonCodes.PERSONA_NOT_MATCHED,
                    "usuário sem perfil; exigido " + restriction.required());
        }

        if (!casa(subject.profileName(), restriction.required())) {
            return RestrictionResult.denied(AccessReasonCodes.PERSONA_NOT_MATCHED,
                    "perfil " + subject.profileName() + " não corresponde à persona "
                            + restriction.required()
                            + (restriction.context() == null || restriction.context().isBlank()
                                    ? "" : " no contexto " + restriction.context()));
        }

        return RestrictionResult.passed("persona atendida pelo perfil " + subject.profileName());
    }

    /**
     * Mapeamento legado de perfil para persona. Fora da tabela, a persona casa quando tem o mesmo
     * nome do perfil.
     */
    private boolean casa(String perfilDoUsuario, List<String> personasExigidas) {
        for (String persona : personasExigidas) {
            switch (persona) {
                case "PLATFORM_ADMIN":
                    if ("ADMIN".equals(perfilDoUsuario) || "PLATFORM_ADMIN".equals(perfilDoUsuario)) {
                        return true;
                    }
                    break;
                case "STORE_ADMIN":
                    if ("STORE_MANAGER".equals(perfilDoUsuario) || "STORE_ADMIN".equals(perfilDoUsuario)) {
                        return true;
                    }
                    break;
                case "CUSTOMER":
                    if ("CUSTOMER".equals(perfilDoUsuario) || "USER".equals(perfilDoUsuario)) {
                        return true;
                    }
                    break;
                case "DRIVER":
                    if ("DRIVER".equals(perfilDoUsuario)) {
                        return true;
                    }
                    break;
                default:
                    if (persona.equals(perfilDoUsuario)) {
                        return true;
                    }
            }
        }
        return false;
    }
}
