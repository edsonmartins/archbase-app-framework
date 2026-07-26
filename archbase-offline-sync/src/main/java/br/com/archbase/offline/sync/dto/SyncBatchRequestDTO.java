package br.com.archbase.offline.sync.dto;

import java.util.ArrayList;
import java.util.List;

/** Corpo do {@code POST /api/v1/sync/operations}. */
public class SyncBatchRequestDTO {
    public String deviceId;
    /** Reservado; o processamento é sempre por-operação (ACK individual). */
    public boolean atomic = false;
    public List<SyncOperationDTO> operations = new ArrayList<>();

    public SyncBatchRequestDTO() {
    }
}
