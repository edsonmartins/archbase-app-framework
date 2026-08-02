package br.com.archbase.security.diagnostics;

import br.com.archbase.security.access.EffectiveCapability;

import java.util.List;

/**
 * O que uma pessoa pode, achatado, com origem e situação por linha.
 *
 * @param administrator quando {@code true}, a lista é <b>irrelevante</b>: a flag encerra a decisão
 *                      antes de qualquer consulta ao catálogo, e nada configurado na tela de
 *                      segurança se aplica a esta conta
 * @param inert         quantas concessões deixariam de valer com
 *                      {@code archbase.security.permission.require-active=true}
 */
public record EffectiveAccessReport(
        String userId,
        String userLabel,
        String profileName,
        List<String> groupNames,
        boolean administrator,
        boolean enabled,
        int granted,
        int effective,
        int inert,
        List<EffectiveCapability> capabilities) {

    public EffectiveAccessReport {
        groupNames = groupNames == null ? List.of() : List.copyOf(groupNames);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }
}
