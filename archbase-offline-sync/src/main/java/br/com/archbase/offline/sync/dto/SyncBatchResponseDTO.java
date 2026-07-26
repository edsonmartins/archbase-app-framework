package br.com.archbase.offline.sync.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resposta do lote: um ACK por operação + contagem por status. */
public class SyncBatchResponseDTO {
    public String serverTime;
    public List<SyncAckDTO> results = new ArrayList<>();
    /** Contagem por status — inclui TODOS os estados (blocked/rejected também). */
    public Map<String, Integer> counts = new LinkedHashMap<>();

    public SyncBatchResponseDTO() {
    }

    /** {@code true} só quando não há nada pendente de ação. */
    public boolean isSuccess() {
        for (SyncAckDTO a : results) {
            if (a.status != SyncOpStatus.PROCESSED && a.status != SyncOpStatus.SKIPPED) {
                return false;
            }
        }
        return true;
    }

    public void recount() {
        counts.clear();
        for (SyncOpStatus s : SyncOpStatus.values()) {
            counts.put(s.name(), 0);
        }
        for (SyncAckDTO a : results) {
            counts.merge(a.status.name(), 1, Integer::sum);
        }
    }
}
