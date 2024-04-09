package br.com.archbase.resource.logger.aspect.integration;

import br.com.archbase.resource.logger.bean.User;
import br.com.archbase.resource.logger.helpers.Utils;
import br.com.archbase.resource.logger.utils.JsonUtil;
import com.google.common.collect.ImmutableMap;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

//@RunWith(SpringRunner.class)
@SuppressWarnings("all")
public class IntegrationTest extends BaseIntegrationTest {

    //public static TestLogger logger = TestLoggerFactory.getTestLogger(IntegrationTest.class);
    // @Autowired
    private MockMvc mvc;

    //@Before
    public void before() {
//        logger.clearAll();
    }

    // @Test
    public void baseTest() throws Exception {
        MvcResult result = mvc.perform(
                get("/getUser")
                        .header("Authorization", Utils.generateBasicAuthToken("username", "{noop}password"))
                        .header("Accept", "application/json")
        ).andReturn();

        User actualUser = JsonUtil.fromJson(result.getResponse().getContentAsString(), User.class);

        List<Map<String, String>> expectedLogMessages = new ArrayList<>();
        expectedLogMessages.add(
                ImmutableMap.of(
                        "level", "INFO",
                        "message", "getUser() chamado via url: [http://localhost/getUser], username: [username]")
        );

        expectedLogMessages.add(
                ImmutableMap.of(
                        "level", "INFO",
                        "message", "getUser\\(\\) levou \\[\\d+ ms\\] para concluir",
                        "type", "regex")
        );

        expectedLogMessages.add(
                ImmutableMap.of(
                        "level", "INFO",
                        "message", "getUser() retornou: [{\"id\":1,\"email\":\"joao@example.com.br\",\"password\":\"secretpassword\"}]")
        );

//        List<Map<String, String>> actualLogMessages = Utils.getFormattedLogEvents(logger);

//        validateLogs(expectedLogMessages, actualLogMessages);

        User expectedUser = new User(1, "joao@example.com.br", "secretpassword");
        assertEquals(expectedUser, actualUser);
    }

    private void validateLogs(List<Map<String, String>> expectedLogMessages, List<Map<String, String>> actualLogMessages) {
        for (int i = 0; i < expectedLogMessages.size(); ++i) {
            assertEquals(expectedLogMessages.get(i).get("level"), actualLogMessages.get(i).get("level"));

            String messageType = expectedLogMessages.get(i).get("type");

            if (messageType != null && messageType.equals("regex")) {
                String expectedPattern = expectedLogMessages.get(i).get("message");
                String actualLogMessage = actualLogMessages.get(i).get("message");

                assertTrue(Pattern.compile(expectedPattern).matcher(actualLogMessage).matches());
            } else {
                assertEquals(expectedLogMessages.get(i).get("message"), actualLogMessages.get(i).get("message"));
            }
        }
    }

}
