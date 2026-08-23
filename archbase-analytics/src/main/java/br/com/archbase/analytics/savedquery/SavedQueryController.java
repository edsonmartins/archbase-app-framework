package br.com.archbase.analytics.savedquery;

import br.com.archbase.analytics.port.SavedQueryStorePort;
import br.com.archbase.analytics.port.SavedQueryStorePort.SavedQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Set;

/**
 * Backend da porta {@code savedQueryStore} da biblioteca de frontend. Formato de
 * fio = {@code SavedQueryRecord} da biblioteca.
 *
 * <p>Autorização: o dono é SEMPRE o usuário autenticado (o {@code ownerId} do
 * corpo é ignorado na escrita). Visibilidade: as próprias + escopo team/org.
 * Remoção: só o dono.
 *
 * <p><b>Quem registra é a autoconfiguração, não o component scan</b> —
 * {@code @ResponseBody} em vez de {@code @RestController} pela mesma razão
 * descrita em {@code AnalyticsProxyController}: como estereótipo, um host que
 * escaneasse {@code br.com.archbase} registrava a classe mesmo com o analytics
 * desligado, e ela subia sem as dependências que a autoconfig publicaria.
 */
@ResponseBody
@RequestMapping("${archbase.analytics.base-path:/api/analytics}/saved-queries")
public class SavedQueryController {

    private static final Logger log = LoggerFactory.getLogger(SavedQueryController.class);
    private static final Set<String> SCOPES = Set.of("private", "team", "org");

    private final SavedQueryStorePort store;
    private final ObjectMapper objectMapper;

    public SavedQueryController(SavedQueryStorePort store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<ObjectNode> list(@RequestParam(value = "scope", required = false) String scope) {
        String filter = SCOPES.contains(scope) ? scope : null;
        return store.listVisible(currentUser(), filter).stream().map(this::toRecord).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjectNode> get(@PathVariable String id) {
        return store.find(id)
                .filter(q -> visibleTo(q, currentUser()))
                .map(q -> ResponseEntity.ok(toRecord(q)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ObjectNode> save(@RequestBody JsonNode body) {
        String user = currentUser();
        String id = body.hasNonNull("id") ? body.get("id").asText() : null;
        JsonNode meta = body.path("meta");
        String name = meta.path("name").asText("Consulta sem nome");
        String scope = meta.path("scope").asText("private");
        if (!SCOPES.contains(scope)) {
            scope = "private";
        }
        if (id != null) {
            SavedQuery existing = store.find(id).orElse(null);
            if (existing != null && !existing.ownerId().equals(user)) {
                return ResponseEntity.status(403).build();
            }
        }
        SavedQuery saved = store.save(id, name, user, scope,
                body.path("schemaVersion").asInt(1),
                body.path("query").toString(),
                body.path("viz").toString());
        return ResponseEntity.ok(toRecord(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable String id) {
        store.remove(id, currentUser());
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════════════════

    private ObjectNode toRecord(SavedQuery q) {
        ObjectNode r = objectMapper.createObjectNode();
        r.put("id", q.id());
        r.put("schemaVersion", q.schemaVersion());
        r.set("query", readJson(q.queryJson()));
        r.set("viz", readJson(q.vizJson()));
        ObjectNode meta = r.putObject("meta");
        meta.put("name", q.name());
        meta.put("ownerId", q.ownerId());
        meta.put("scope", q.scope());
        r.put("createdAt", q.createdAt().toString());
        r.put("updatedAt", q.updatedAt().toString());
        return r;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Analytics: payload de consulta salva ilegível.", e);
            return objectMapper.createObjectNode();
        }
    }

    private static boolean visibleTo(SavedQuery q, String user) {
        return q.ownerId().equals(user) || !"private".equals(q.scope());
    }

    private static String currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getName() != null ? a.getName() : "desconhecido";
    }
}
