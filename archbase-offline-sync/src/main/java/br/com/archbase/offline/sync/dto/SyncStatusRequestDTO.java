package br.com.archbase.offline.sync.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Corpo do {@code POST /api/v1/sync/operations/status}: os números de transação
 * ({@code operationId}) que o cliente ainda não confirmou e quer reconciliar.
 */
public class SyncStatusRequestDTO {
    public List<String> operationIds = new ArrayList<>();

    public SyncStatusRequestDTO() {
    }
}
