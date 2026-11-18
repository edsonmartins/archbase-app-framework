package br.com.archbase.controlplane.provisioning;

import java.util.List;

/**
 * Manifesto de features do produto: as chaves de entitlement que ele entende. Opcional — sem bean, o manifesto vai
 * vazio. Publicado junto com os presets no primeiro registro e a cada versão nova do produto.
 *
 * @since 3.4.6
 */
public interface FeatureManifestProvider {

    List<FeatureDescriptor> features();

    /**
     * @param key         chave do entitlement (ex.: {@code powerview.export_excel})
     * @param type        {@code BOOLEAN}, {@code LIMIT} ou {@code METERED}
     * @param description descrição
     */
    record FeatureDescriptor(String key, String type, String description) {
    }
}
