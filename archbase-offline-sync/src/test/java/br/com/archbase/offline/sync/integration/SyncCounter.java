package br.com.archbase.offline.sync.integration;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Entidade de teste para observar o efeito (commit/rollback) dos handlers. */
@Entity(name = "SyncCounter")
class SyncCounter {
    @Id
    String id;

    SyncCounter() {
    }

    SyncCounter(String id) {
        this.id = id;
    }
}
