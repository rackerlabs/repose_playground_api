package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import factories.TestFactory;
import models.TestRequest;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import play.api.routing.Router;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;

import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;
import static play.mvc.Results.ok;

/**
 * Created by dimi5963 on 3/10/16.
 */
public class TestClientImplTest {

    private WSClient ws;
    private Server server;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @After
    public void tearDown() {
        ws.close();
        server.stop();
    }

    @Test
    public void testMakeTestRequest() throws Exception {
        TestRequest testRequest = new TestRequest("GET", "/test", "", new HashMap<String, String>(){
            {
                put("name", "value");
            }
        });

        TestFactory testFactory = mock(TestFactory.class);

        when(testFactory.getTestUrl(anyString(), anyString(), anyString())).thenReturn("/test");

        Router router = new RoutingDsl()
                .GET("/test").routeTo(() -> {
                    ArrayNode response = Json.newArray();
                    ObjectNode jsonNode = Json.newObject();
                    response.add(jsonNode.put("name", "1.0"));
                    jsonNode = Json.newObject();
                    response.add(jsonNode.put("name", "2.0"));
                    return ok(Json.toJson(response));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        TestClientImpl testClient = new TestClientImpl(testFactory);
        testClient.wsClient = ws;

        JsonNode response = testClient.makeTestRequest(testRequest, "127.0.0.1", "8080");
        assertEquals("[{\"name\":\"1.0\"},{\"name\":\"2.0\"}]", response.get("responseBody").asText());
        assertEquals(200, response.get("responseStatus").asInt());
        assertEquals("OK", response.get("responseStatusText").asText());
        assertEquals(3, response.get("responseHeaders").size());

        verify(testFactory, times(1)).getTestUrl(anyString(), anyString(), anyString());
    }

    @Test
    public void testMakeTestFailure() throws Exception {
        TestRequest testRequest = new TestRequest("GET", "/test-not-found", "", new HashMap<String, String>(){
            {
                put("name", "value");
            }
        });

        TestFactory testFactory = mock(TestFactory.class);

        when(testFactory.getTestUrl(anyString(), anyString(), anyString())).thenReturn("/test-not-found");

        Router router = new RoutingDsl()
                .GET("/test").routeTo(() -> {
                    ArrayNode response = Json.newArray();
                    ObjectNode jsonNode = Json.newObject();
                    response.add(jsonNode.put("name", "1.0"));
                    jsonNode = Json.newObject();
                    response.add(jsonNode.put("name", "2.0"));
                    return ok(Json.toJson(response));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        TestClientImpl testClient = new TestClientImpl(testFactory);
        testClient.wsClient = ws;

        JsonNode response = testClient.makeTestRequest(testRequest, "127.0.0.1", "8080");
        assertNotEquals("[{\"name\":\"1.0\"},{\"name\":\"2.0\"}]", response.get("responseBody").asText());
        assertEquals(404, response.get("responseStatus").asInt());
        assertEquals("Not Found", response.get("responseStatusText").asText());
        assertEquals(3, response.get("responseHeaders").size());

        verify(testFactory, times(1)).getTestUrl(anyString(), anyString(), anyString());
    }
}