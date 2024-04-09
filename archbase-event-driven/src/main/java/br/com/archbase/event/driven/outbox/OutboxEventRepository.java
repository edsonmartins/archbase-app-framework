package br.com.archbase.event.driven.outbox;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends CrudRepository<OutboxEvent, Long> {
}
