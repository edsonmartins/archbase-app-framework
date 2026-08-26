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

    /**
     * A consulta de {@code id}, quando ela pode ser mostrada a {@code requesterId}.
     *
     * <p><b>Implemente isto se a sua aplicação tem mais de um tenant.</b> O controlador busca por id
     * e decide a visibilidade por dono e escopo — e escopo {@code team}/{@code org} não tem
     * dimensão de tenant em lugar nenhum deste contrato. Com o {@code find(String)} puro, quem
     * souber o id de uma consulta compartilhada de outro tenant consegue lê-la, e a persistência
     * não tem como interceptar, porque recebe só o id.
     *
     * <p>O padrão delega para {@link #find(String)}, preservando o comportamento de sempre: nenhuma
     * implementação existente muda ao atualizar. Sobrescreva para estreitar ao tenant corrente —
     * a sua implementação tem acesso ao contexto que este módulo, de propósito, não tem.
     */
    default Optional<SavedQuery> findVisibleTo(String id, String requesterId) {
        return find(id);
    }

    /** Insere quando {@code id} é nulo; atualiza quando existe e o dono confere. */
    SavedQuery save(String id, String name, String ownerId, String scope,
                    int schemaVersion, String queryJson, String vizJson);

    boolean remove(String id, String ownerId);
}
