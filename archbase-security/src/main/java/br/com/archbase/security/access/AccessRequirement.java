package br.com.archbase.security.access;

import java.util.List;

/**
 * O que está sendo pedido: a capacidade, o escopo em que ela precisa valer e as trancas declaradas.
 *
 * <p>Um requisito pode ter capacidade, restrições, ou os dois:
 *
 * <ul>
 *   <li><b>Só capacidade</b> — {@code @HasPermission} sozinho. Decide o catálogo.</li>
 *   <li><b>Só restrições</b> — {@code @RequireProfile} sozinho. Passar nas trancas <b>concede</b>,
 *       porque não há capacidade a consultar. É por isso que uma anotação de restrição sozinha
 *       não substitui {@code @HasPermission}: ela vira o portão inteiro, e quem satisfaz o perfil
 *       passa sem que ninguém tenha concedido nada.</li>
 *   <li><b>Os dois</b> — {@code @RequireProfile(resource = ...)}, ou as duas anotações no mesmo
 *       método. As trancas negam antes; o catálogo concede depois.</li>
 * </ul>
 *
 * @param origin assinatura do método que originou o requisito, para diagnóstico e para o aviso
 *               único por método de {@code @RequireRole} sem resolver
 */
public record AccessRequirement(
        String resource,
        String action,
        String tenantId,
        String companyId,
        String projectId,
        List<Restriction> restrictions,
        String origin) {

    public AccessRequirement {
        restrictions = restrictions == null ? List.of() : List.copyOf(restrictions);
    }

    public static AccessRequirement of(String resource, String action) {
        return of(resource, action, null, null, null);
    }

    public static AccessRequirement of(String resource, String action,
                                       String tenantId, String companyId, String projectId) {
        return new AccessRequirement(resource, action, tenantId, companyId, projectId, List.of(), null);
    }

    /** Requisito de tranca pura, sem capacidade a consultar no catálogo. */
    public static AccessRequirement ofRestrictions(String origin, Restriction... restrictions) {
        return new AccessRequirement(null, null, null, null, null, List.of(restrictions), origin);
    }

    /** Requisito de tranca somada a uma capacidade. */
    public static AccessRequirement ofRestrictionsAndCapability(String origin, String resource, String action,
                                                                Restriction... restrictions) {
        return new AccessRequirement(resource, action, null, null, null, List.of(restrictions), origin);
    }

    public AccessRequirement withOrigin(String origin) {
        return new AccessRequirement(resource, action, tenantId, companyId, projectId, restrictions, origin);
    }

    public boolean hasCapability() {
        return resource != null && !resource.isBlank() && action != null && !action.isBlank();
    }

    /** A capacidade na forma {@code recurso:acao}, para log e diagnóstico. */
    public String capability() {
        return resource + ":" + action;
    }

    /** Como identificar este requisito em log. */
    public String label() {
        if (hasCapability()) {
            return capability();
        }
        return origin == null ? "restrições" : origin;
    }
}
