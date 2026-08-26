package br.com.archbase.analytics;

import br.com.archbase.analytics.port.SavedQueryStorePort;
import br.com.archbase.analytics.savedquery.SavedQueryController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sem usuário autenticado, o analytics recusa em vez de inventar um.
 *
 * <p>Antes, {@code currentUser()} devolvia a string {@code "desconhecido"} quando não havia
 * autenticação, e esse nome seguia como se fosse gente: virava dono de consulta salva, critério de
 * visibilidade e sujeito do token de escopo. Todos os anônimos compartilhavam uma identidade — viam
 * as consultas uns dos outros e podiam removê-las.
 *
 * <p>Nunca aconteceu em produção porque nada torna estas rotas públicas. O problema é a proteção
 * depender disso: bastava alguém acrescentar {@code /api/analytics/**} a uma whitelist para o
 * buraco abrir sem que nenhuma linha deste módulo mudasse.
 */
class AnalyticsExigeAutenticacaoTest {

    private final SavedQueryController controller =
            new SavedQueryController(new StoreDeMentira(), new ObjectMapper());

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sem autenticação nenhuma, listar responde 401")
    void semAutenticacaoNaoLista() {
        assertThat(controller.list(null).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("token anônimo do Spring não vale como usuário")
    void anonimoNaoVale() {
        // AnonymousAuthenticationToken tem isAuthenticated() == true e getName() == "anonymousUser":
        // sem o teste de tipo, ele passaria como qualquer outra identidade.
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "chave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(controller.list(null).getStatusCode().value()).isEqualTo(401);
        assertThat(controller.get("qualquer").getStatusCode().value()).isEqualTo(401);
        assertThat(controller.remove("qualquer").getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("listar sem o parâmetro de escopo não estoura")
    void listarSemEscopoFunciona() {
        // Regressão: SCOPES é um Set.of(), e Set.of().contains(null) lança NullPointerException em
        // vez de devolver false. Como scope é opcional, a chamada mais comum da tela — listar tudo —
        // respondia 500. Este teste encontrou o defeito; o assert é que a resposta seja 200.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "n/a",
                        AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertThat(controller.list(null).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("usuário autenticado passa e enxerga o que é seu")
    void autenticadoPassa() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "n/a",
                        AuthorityUtils.createAuthorityList("ROLE_USER")));

        var resposta = controller.list(null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    /** Store mínima: devolve uma consulta de "alice" e registra o que lhe perguntam. */
    private static final class StoreDeMentira implements SavedQueryStorePort {

        private final SavedQuery daAlice = new SavedQuery(
                "id-1", "Minha consulta", "alice", "private", 1, "{}", "{}",
                Instant.EPOCH, Instant.EPOCH);

        @Override
        public List<SavedQuery> listVisible(String ownerId, String scope) {
            return "alice".equals(ownerId) ? List.of(daAlice) : List.of();
        }

        @Override
        public Optional<SavedQuery> find(String id) {
            return "id-1".equals(id) ? Optional.of(daAlice) : Optional.empty();
        }

        @Override
        public SavedQuery save(String id, String name, String ownerId, String scope,
                               int schemaVersion, String queryJson, String vizJson) {
            return daAlice;
        }

        @Override
        public boolean remove(String id, String ownerId) {
            return false;
        }
    }
}
