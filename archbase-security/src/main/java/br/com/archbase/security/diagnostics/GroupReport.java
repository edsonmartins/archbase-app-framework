package br.com.archbase.security.diagnostics;

import java.util.List;

/**
 * Quem está no grupo e o que cada pessoa acaba tendo.
 *
 * <p><b>Por que os dois juntos.</b> Ver só o que o grupo concede engana: ninguém tem <i>apenas</i> o
 * que o grupo dá. A pessoa acumula o perfil, os outros grupos e as concessões diretas — e é o total
 * que decide se ela consegue fazer algo. Um grupo com três capacidades pode ter um membro que faz
 * tudo, por outra via.
 *
 * <p>Por isso cada membro traz o próprio total, calculado pelo mesmo leitor de capacidades que a
 * tela de efetivo usa: o número aqui e o da tela de detalhe são o mesmo número, e não duas contas
 * que divergem.
 *
 * @param grants     o que o grupo concede — a parte que é responsabilidade dele
 * @param members    quem está dentro, cada um com o que acumula de todas as origens
 */
public record GroupReport(
        String groupId,
        String groupName,
        String description,
        List<EffectiveCapabilityLine> grants,
        List<Member> members) {

    public GroupReport {
        grants = grants == null ? List.of() : List.copyOf(grants);
        members = members == null ? List.of() : List.copyOf(members);
    }

    /**
     * Uma capacidade concedida ao grupo.
     *
     * @param situation por que vale ou não vale — inerte aqui significa que o grupo concede algo
     *                  que não produz efeito, e é a explicação de "concedi e não funcionou"
     */
    public record EffectiveCapabilityLine(String resource, String action, String situation) {
    }

    /**
     * Um membro do grupo, com o que ele acumula de <b>todas</b> as origens.
     *
     * @param effective quantas capacidades de fato valem para ele
     * @param inert     quantas foram concedidas e não produzem efeito
     * @param denied    quantas um portão anterior ao GRANT recusa
     * @param administrator quando verdadeiro, os números acima são irrelevantes: administrador passa
     *                      direto pelo portão GRANT, e nada configurado nesta tela se aplica a ele
     */
    public record Member(
            String userId,
            String name,
            String email,
            String profileName,
            boolean administrator,
            boolean enabled,
            int total,
            int effective,
            int inert,
            int denied) {
    }
}
