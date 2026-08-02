package br.com.archbase.security.access;

/**
 * O que está sendo pedido: a capacidade e o escopo em que ela precisa valer.
 *
 * <p>Nos portões que ainda não existem — {@link Gate#RESTRICTION} e {@link Gate#LEVEL} — o
 * requisito ganhará campos próprios. Por ora carrega o que a decisão de hoje usa.
 *
 * @param resource nome do recurso no catálogo
 * @param action   nome da ação no catálogo
 */
public record AccessRequirement(
        String resource,
        String action,
        String tenantId,
        String companyId,
        String projectId) {

    public static AccessRequirement of(String resource, String action) {
        return new AccessRequirement(resource, action, null, null, null);
    }

    public static AccessRequirement of(String resource, String action,
                                       String tenantId, String companyId, String projectId) {
        return new AccessRequirement(resource, action, tenantId, companyId, projectId);
    }

    /** A capacidade na forma {@code recurso:acao}, para log e diagnóstico. */
    public String capability() {
        return resource + ":" + action;
    }
}
