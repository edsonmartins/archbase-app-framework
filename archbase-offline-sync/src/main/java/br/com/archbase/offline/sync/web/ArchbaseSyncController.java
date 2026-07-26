package br.com.archbase.offline.sync.web;

import br.com.archbase.offline.sync.dto.SyncBatchRequestDTO;
import br.com.archbase.offline.sync.dto.SyncBatchResponseDTO;
import br.com.archbase.offline.sync.service.SyncOperationProcessor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint base do protocolo batch: {@code POST /api/v1/sync/operations}.
 * Reusável — o app não precisa reescrever o controller. Registrado pelo starter.
 */
@RestController
@RequestMapping("/api/v1/sync")
public class ArchbaseSyncController {

    private final SyncOperationProcessor processor;
    private final AppVersionGate versionGate;

    public ArchbaseSyncController(SyncOperationProcessor processor,
                                  AppVersionGate versionGate) {
        this.processor = processor;
        this.versionGate = versionGate;
    }

    @PostMapping(value = "/operations",
            consumes = "application/json", produces = "application/json")
    public SyncBatchResponseDTO operations(
            @RequestHeader(value = "X-App-Version", required = false) String appVersion,
            @RequestBody SyncBatchRequestDTO request) {
        versionGate.check(appVersion); // lança 426 se incompatível
        return processor.process(request);
    }
}
