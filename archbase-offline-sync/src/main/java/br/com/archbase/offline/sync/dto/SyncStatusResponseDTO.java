package br.com.archbase.offline.sync.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resposta do {@code POST /api/v1/sync/operations/status}: dos ids pedidos,
 * quais JÁ FORAM PROCESSADOS no servidor (estão no ledger). O cliente marca
 * esses como enviados; os ausentes ele reenvia.
 */
public class SyncStatusResponseDTO {
    public List<String> processed = new ArrayList<>();

    public SyncStatusResponseDTO() {
    }

    public SyncStatusResponseDTO(List<String> processed) {
        this.processed = processed;
    }
}
