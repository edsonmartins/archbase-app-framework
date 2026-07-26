package br.com.archbase.offline.sync.integration;

import org.springframework.data.jpa.repository.JpaRepository;

interface SyncCounterRepository extends JpaRepository<SyncCounter, String> {
}
