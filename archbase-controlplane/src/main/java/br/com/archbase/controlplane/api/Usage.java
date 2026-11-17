package br.com.archbase.controlplane.api;

/**
 * Resultado de {@link Entitlements#consume}: o consumo é sempre registrado; estouro é tolerado e informado.
 *
 * @param key       entitlement consumido
 * @param consumed  quantidade desta chamada
 * @param used      total usado (contabilizado pelo Control Plane + local ainda não reportado)
 * @param limit     limite do entitlement
 * @param remaining {@code limit - used}, nunca negativo
 * @param exceeded  se o total passou do limite
 * @since 3.4.6
 */
public record Usage(String key, long consumed, long used, long limit, long remaining, boolean exceeded) {
}
