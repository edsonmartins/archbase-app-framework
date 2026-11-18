package br.com.archbase.controlplane.provisioning;

import java.util.List;

/**
 * Um preset de criação de tenant oferecido pelo produto.
 *
 * @param id          identificador estável
 * @param version     versão
 * @param name        nome legível
 * @param description descrição
 * @param parameters  nomes dos parâmetros aceitos
 * @since 3.4.6
 */
public record PresetDescriptor(String id, String version, String name, String description, List<String> parameters) {

    public PresetDescriptor {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
