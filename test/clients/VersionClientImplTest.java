package clients;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import factories.VersionFactory;
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

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

/**
 * Created by dimi5963 on 3/8/16.
 */
public class VersionClientImplTest {

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
    public void testGetVersionsSuccess() throws Exception {
        VersionFactory versionFactory = mock(VersionFactory.class);

        when(versionFactory.getVersionUrl()).thenReturn("/versions");
        when(versionFactory.getVersionAuthToken()).thenReturn("token");

        Router router = new RoutingDsl()
                .GET("/versions").routeTo(() -> {
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
        VersionClientImpl versionClient = new VersionClientImpl(versionFactory);
        versionClient.wsClient = ws;

        try {
            List<String> versions = versionClient.getVersions();
            assertEquals(2, versions.size());
            assertTrue(versions.stream().anyMatch(t -> t.equals("1.0")));
        }catch(InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        verify(versionFactory, times(1)).getVersionUrl();
        verify(versionFactory, times(1)).getVersionAuthToken();

    }

    @Test
    public void testGetVersions404() throws Exception {
        VersionFactory versionFactory = mock(VersionFactory.class);

        when(versionFactory.getVersionUrl()).thenReturn("/versions");
        when(versionFactory.getVersionAuthToken()).thenReturn("token");

        Router router = new RoutingDsl()
                .GET("/versions").routeTo(() -> {
                    return notFound();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        VersionClientImpl versionClient = new VersionClientImpl(versionFactory);
        versionClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("We are currently experiencing difficulties.  Please try again later.");
        versionClient.getVersions();

        verify(versionFactory, times(1)).getVersionUrl();
        verify(versionFactory, times(1)).getVersionAuthToken();
    }

    @Test
    public void testGetVersionsEmptyResponse() throws Exception {
        VersionFactory versionFactory = mock(VersionFactory.class);

        when(versionFactory.getVersionUrl()).thenReturn("/versions");
        when(versionFactory.getVersionAuthToken()).thenReturn("token");

        Router router = new RoutingDsl()
                .GET("/versions").routeTo(() -> {
                    return ok();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        VersionClientImpl versionClient = new VersionClientImpl(versionFactory);
        versionClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("We are currently experiencing difficulties.  Please try again later.");
        versionClient.getVersions();

        verify(versionFactory, times(1)).getVersionUrl();
        verify(versionFactory, times(1)).getVersionAuthToken();
    }
}