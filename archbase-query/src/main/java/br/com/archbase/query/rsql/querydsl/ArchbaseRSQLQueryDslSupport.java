package br.com.archbase.query.rsql.querydsl;

import br.com.archbase.query.rsql.common.RSQLOperators;
import br.com.archbase.query.rsql.common.RSQLOrBypassAnalyzer;
import br.com.archbase.query.rsql.parser.ast.Node;
import br.com.archbase.query.rsql.jpa.ArchbaseRSQLJPASupport;
import br.com.archbase.query.rsql.parser.RSQLParser;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import java.util.Map;

@Slf4j
@SuppressWarnings({"rawtypes"})
public class ArchbaseRSQLQueryDslSupport extends ArchbaseRSQLJPASupport {

    public ArchbaseRSQLQueryDslSupport() {
        super();
    }

    public ArchbaseRSQLQueryDslSupport(Map<String, EntityManager> entityManagerMap) {
        super(entityManagerMap);
    }

    public static BooleanExpression toPredicate(final String rsqlQuery, final Path qClazz) {
        return toPredicate(rsqlQuery, qClazz, null);
    }

    public static BooleanExpression toPredicate(final String rsqlQuery, final Path qClazz, final Map<String, String> propertyPathMapper) {
        log.debug("toPredicate({},qClazz:{},propertyPathMapper:{})", rsqlQuery, qClazz);
        if (StringUtils.hasText(rsqlQuery)) {
            Node rsql = new RSQLParser(RSQLOperators.supportedOperators()).parse(rsqlQuery);
            // Como no caminho JPA. Até a 3.5.0 o QueryDSL não conferia: um seletor protegido sob OR passava.
            RSQLOrBypassAnalyzer.assertNotBypassable(rsql, getProtectedSelectors().get(qClazz.getType()));
            return rsql.accept(new ArchbaseRSQLQueryDslPredicateConverter(propertyPathMapper), qClazz);
        } else {
            return null;
        }
    }


}
