package br.com.archbase.analytics.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistência das consultas salvas do explorador — o par servidor da porta
 * {@code savedQueryStore} da biblioteca de frontend.
 *
 * <p>O payload ({@code queryJson} / {@code vizJson}) é OPACO: o formato pertence
 * à biblioteca de frontend, versionado por {@code schemaVersion}. O framework
 * guarda, lista e devolve — nunca interpreta. O produto fornece a persistência.
 */
public interface SavedQueryStorePort {

    record SavedQuery(
            String id,
            String name,
            String ownerId,
            /** private | team | org. */
            String scope,
            int schemaVersion,
            String queryJson,
            String vizJson,
            Instant createdAt,
            Instant updatedAt) {
    }

    /**
     * As consultas visíveis ao usuário: as próprias (qualquer escopo) mais as de
     * escopo team/org de qualquer dono. {@code scope} opcional filtra.
     */
    List<SavedQuery> listVisible(String ownerId, String scope);

    Optional<SavedQuery> find(String id);

    /** Insere quando {@code id} é nulo; atualiza quando existe e o dono confere. */
    SavedQuery save(String id, String name, String ownerId, String scope,
                    int schemaVersion, String queryJson, String vizJson);

    boolean remove(String id, String ownerId);
}
