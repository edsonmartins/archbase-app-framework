package br.com.archbase.offline.sync.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProcessedSyncOperationRepository
        extends JpaRepository<ProcessedSyncOperation, ProcessedSyncOperation.Pk> {

    Optional<ProcessedSyncOperation> findByTenantIdAndOperationId(String tenantId,
                                                                  String operationId);

    boolean existsByTenantIdAndOperationId(String tenantId, String operationId);

    @Modifying
    @Query("delete from ProcessedSyncOperation p where p.processedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
