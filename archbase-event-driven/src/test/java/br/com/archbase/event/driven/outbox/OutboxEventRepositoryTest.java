package br.com.archbase.event.driven.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;

import java.lang.reflect.ParameterizedType;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxEventRepositoryTest {

    @Test
    void repositoryUsesUuidAsIdType() {
        ParameterizedType repositoryType = (ParameterizedType) OutboxEventRepository.class.getGenericInterfaces()[0];

        assertEquals(CrudRepository.class, repositoryType.getRawType());
        assertEquals(OutboxEvent.class, repositoryType.getActualTypeArguments()[0]);
        assertEquals(UUID.class, repositoryType.getActualTypeArguments()[1]);
    }
}
