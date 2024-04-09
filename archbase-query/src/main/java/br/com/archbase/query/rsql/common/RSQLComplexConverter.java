package br.com.archbase.query.rsql.common;

import br.com.archbase.query.rsql.parser.ast.AndNode;
import br.com.archbase.query.rsql.parser.ast.ComparisonNode;
import br.com.archbase.query.rsql.parser.ast.ComparisonOperator;
import br.com.archbase.query.rsql.parser.ast.OrNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RSQLComplexConverter extends RSQLVisitorBase<Void, Map<String, MultiValueMap<String, String>>> {

    public static final String VISIT_NODE_MAP = "visit(node:{},map:{})";

    public RSQLComplexConverter() {
        super();
    }

    @Override
    public Void visit(ComparisonNode node, Map<String, MultiValueMap<String, String>> map) {
        log.debug(VISIT_NODE_MAP, node, map);
        String key = node.getSelector();
        ComparisonOperator operator = node.getOperator();
        MultiValueMap<String, String> operatorMap = map.computeIfAbsent(key, k -> CollectionUtils.toMultiValueMap(new HashMap<>()));
        for (String ops : operator.getSymbols()) {
            operatorMap.addAll(ops, node.getArguments());
        }
        return null;
    }

    @Override
    public Void visit(AndNode node, Map<String, MultiValueMap<String, String>> map) {
        log.debug(VISIT_NODE_MAP, node, map);
        node.getChildren().forEach(n -> n.accept(this, map));
        return null;
    }

    @Override
    public Void visit(OrNode node, Map<String, MultiValueMap<String, String>> map) {
        log.debug(VISIT_NODE_MAP, node, map);
        node.getChildren().forEach(n -> n.accept(this, map));
        return null;
    }

    @Override
    public Map<String, String> getPropertyPathMapper() {
        return Collections.emptyMap();
    }

}
