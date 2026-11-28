package teste.offline.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O lote chega pelo HTTP de uma aplicação Boot 4, cujo Spring MVC lê o corpo com o Jackson 3. O payload de
 * SyncOperationDTO é um JsonNode do Jackson 2: até a 3.5.0 o Jackson 3 não o construía e todo lote com payload
 * era recusado com 400 antes de chegar ao handler.
 */
@DisplayName("lote de sync pelo HTTP")
class LotePeloHttpTest {

    @Test
    @DisplayName("o handler recebe o payload do lote enviado por POST")
    void payloadChegaAoHandler() throws Exception {
        try (ConfigurableApplicationContext ctx = new SpringApplication(AppHttp.class).run(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:offline-http;DB_CLOSE_DELAY=-1",
                "--spring.jpa.hibernate.ddl-auto=create-drop")) {
            String porta = ctx.getEnvironment().getProperty("local.server.port");
            String lote = """
                    {"deviceId":"d1","operations":[{"id":"op-http-1","type":"LE_VALOR","aggregateId":"a1",
                     "payload":{"valor":42}}]}""";
            HttpResponse<String> resposta = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/v1/sync/operations"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(lote)).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(200);
            assertThat(resposta.body()).contains("\"PROCESSED\"").contains("\"serverVersion\":42");
        }
    }
}
