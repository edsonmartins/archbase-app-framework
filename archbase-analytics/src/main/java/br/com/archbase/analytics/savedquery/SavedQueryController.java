package br.com.archbase.analytics.savedquery;

import br.com.archbase.analytics.port.SavedQueryStorePort;
import br.com.archbase.analytics.port.SavedQueryStorePort.SavedQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
    public ResponseEntity<List<ObjectNode>> list(
            @RequestParam(value = "scope", required = false) String scope) {
        String user = currentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        // O teste de nulo vem antes de propósito: SCOPES é um Set.of(), e Set.of().contains(null)
        // lança NullPointerException em vez de responder false. Como o parâmetro é opcional, a
        // chamada sem escopo — a mais comum da tela — virava 500.
        String filter = scope != null && SCOPES.contains(scope) ? scope : null;
        return ResponseEntity.ok(
                store.listVisible(user, filter).stream().map(this::toRecord).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObjectNode> get(@PathVariable String id) {
        String user = currentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        // findVisibleTo em vez de find: é o gancho por onde uma implementação multi-tenant estreita
        // a busca ao tenant de quem pergunta. O padrão do port delega ao find de sempre.
        return store.findVisibleTo(id, user)
                .filter(q -> visibleTo(q, user))
                .map(q -> ResponseEntity.ok(toRecord(q)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ObjectNode> save(@RequestBody JsonNode body) {
        String user = currentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        String id = body.hasNonNull("id") ? body.get("id").asText() : null;
        JsonNode meta = body.path("meta");
        String name = meta.path("name").asText("Consulta sem nome");
        String scope = meta.path("scope").asText("private");
        if (!SCOPES.contains(scope)) {
            scope = "private";
        }
        if (id != null) {
            // Id que não existe segue criando o registro com a chave escolhida pelo cliente. Não é
            // descuido: recusar seria o mais seguro, mas quebraria cliente que gera o id do próprio
            // lado — o que é a norma em fluxo offline-first, e este framework tem um. Quem persiste
            // é que precisa tratar o id recebido como dado de entrada, não como chave confiável.
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

    /**
     * Remove a consulta, se ela for de quem pediu.
     *
     * <p>Responde 204 tanto para removida quanto para inexistente ou de outro dono: o port já
     * recusa o que não é do solicitante, e distinguir os casos na resposta contaria a um estranho
     * que aquele id existe.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable String id) {
        String user = currentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        store.remove(id, user);
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

    /**
     * O usuário autenticado, ou {@code null}.
     *
     * <p>Antes, sem autenticação isto devolvia a string {@code "desconhecido"}, que então virava
     * dono e critério de visibilidade como qualquer outro nome: todos os anônimos compartilhavam
     * uma identidade, viam as consultas uns dos outros e podiam removê-las. Nunca aconteceu porque
     * nada torna estas rotas públicas — mas era o tipo de proteção que depende de configuração
     * alheia continuar como está.
     */
    private static String currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a.getName() == null
                || a instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return a.getName();
    }
}
