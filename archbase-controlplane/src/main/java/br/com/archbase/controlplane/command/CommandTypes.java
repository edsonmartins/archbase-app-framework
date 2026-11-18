package br.com.archbase.controlplane.command;

import java.util.Set;

/**
 * Tipos de comando.
 *
 * @since 3.4.6
 */
public final class CommandTypes {

    public static final String TENANT_PROVISION = "tenant.provision";
    public static final String TENANT_UPDATE = "tenant.update";
    public static final String TENANT_SUSPEND = "tenant.suspend";
    public static final String TENANT_RESUME = "tenant.resume";
    public static final String TENANT_DEPROVISION = "tenant.deprovision";
    public static final String TENANT_RESET = "tenant.reset";

    /** Do próprio módulo: atualizam os caches sem chamar o produto. */
    public static final String LICENSE_RENEWED = "license.renewed";
    public static final String ENTITLEMENTS_CHANGED = "entitlements.changed";
    public static final String MEMBERSHIP_CHANGED = "membership.changed";

    public static final Set<String> PRODUCT = Set.of(TENANT_PROVISION, TENANT_UPDATE, TENANT_SUSPEND, TENANT_RESUME,
            TENANT_DEPROVISION, TENANT_RESET);

    public static final Set<String> MODULE = Set.of(LICENSE_RENEWED, ENTITLEMENTS_CHANGED, MEMBERSHIP_CHANGED);

    private CommandTypes() {
    }
}
