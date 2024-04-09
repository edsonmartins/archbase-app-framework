package br.com.archbase.query.rsql.common;

import br.com.archbase.query.rsql.querydsl.ArchbaseRSQLQueryDslSupport;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.EntityManager;
import java.util.Map;

@Slf4j
public class RSQLSupport extends ArchbaseRSQLQueryDslSupport {

    public RSQLSupport(Map<String, EntityManager> entityManagerMap) {
        super(entityManagerMap);
        log.info("RSQLSupport foi inicializado.");
    }


}
