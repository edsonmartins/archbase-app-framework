package br.com.archbase.query.rsql.common;

import br.com.archbase.query.rsql.parser.ast.AndNode;
import br.com.archbase.query.rsql.parser.ast.ComparisonNode;
import br.com.archbase.query.rsql.parser.ast.OrNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.Map;

@Slf4j
public class RSQLSimpleConverter extends RSQLVisitorBase<Void, MultiValueMap<String, String>> {

    public static final String VISIT_NODE_MAP = "visit(node:{},map:{})";

    public RSQLSimpleConverter() {
        super();
    }

    @Override
    public Void visit(ComparisonNode node, MultiValueMap<String, String> map) {
        log.debug(VISIT_NODE_MAP, node, map);
        map.addAll(node.getSelector(), node.getArguments());
        return null;
    }

    @Override
    public Void visit(AndNode node, MultiValueMap<String, String> map) {
        log.debug(VISIT_NODE_MAP, node, map);
        node.getChildren().forEach(n -> n.accept(this, map));
        return null;
    }

    @Override
    public Void visit(OrNode node, MultiValueMap<String, String> map) {
        log.debug(VISIT_NODE_MAP, node, map);
        node.getChildren().forEach(n -> n.accept(this, map));
        return null;
    }

    @Override
    public Map<String, String> getPropertyPathMapper() {
        return Collections.emptyMap();
    }

}
