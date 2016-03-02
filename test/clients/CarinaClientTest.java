package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import factories.ICarinaFactory;
import factories.IClusterFactory;
import models.Cluster;
import models.User;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static junit.framework.Assert.*;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.mvc.Results.*;

/**
 * Created by dimi5963 on 3/1/16.
 */
public class CarinaClientTest {

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
    public void testCreateClusterSuccess() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaUserUrl(anyString())).thenReturn("/test-cluster-create");

        Router router = new RoutingDsl()
                .POST("/test-cluster-create").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "active");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        try {
            assertTrue(carinaClient.createCluster("fake-name", user));
        }catch(InterruptedException | InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        verify(clusterFactoryMock, times(1)).getCarinaUserUrl(anyString());
        verify(clusterFactoryMock, never()).getCarinaClusterUrl(anyString(), anyString());
    }

    @Test
    public void testCreateClusterUserNull() throws Exception {
        User user = null;

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaUserUrl(anyString())).thenReturn("/test-cluster-create");

        Router router = new RoutingDsl()
                .POST("/test-cluster-create").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "active");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Required parameters were no provided.");
        carinaClient.createCluster("fake-name", user);

        verify(clusterFactoryMock, never()).getCarinaUserUrl(anyString());
        verify(clusterFactoryMock, never()).getCarinaClusterUrl(anyString(), anyString());
    }

    @Test
    public void testCreateClusterClusterNull() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaUserUrl(anyString())).thenReturn("/test-cluster-create");

        Router router = new RoutingDsl()
                .POST("/test-cluster-create").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "active");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Required parameters were no provided.");
        carinaClient.createCluster(null, user);

        verify(clusterFactoryMock, times(1)).getCarinaUserUrl(anyString());
        verify(clusterFactoryMock, never()).getCarinaClusterUrl(anyString(), anyString());
    }

    @Test
    public void testCreateClusterCarinaUrlNull() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaUserUrl(anyString())).thenReturn(null);

        Router router = new RoutingDsl()
                .POST("/test-cluster-create").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "active");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Carina user url is misconfigured.");
        carinaClient.createCluster("fake-name", user);

        verify(clusterFactoryMock, times(1)).getCarinaUserUrl(anyString());
        verify(clusterFactoryMock, never()).getCarinaClusterUrl(anyString(), anyString());
    }

    @Test
    public void testCreateClusterRequest404() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaUserUrl(anyString())).thenReturn("/test-cluster-create");

        Router router = new RoutingDsl()
                .POST("/test-cluster-create").routeTo(() -> {
                    return notFound();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("We are currently experiencing difficulties.  Please try again later.");
        carinaClient.createCluster("fake-name", user);

        verify(clusterFactoryMock, times(1)).getCarinaUserUrl(anyString());
        verify(clusterFactoryMock, never()).getCarinaClusterUrl(anyString(), anyString());

    }

    @Test
    public void testCreateClusterRequest500() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaUserUrl(anyString())).thenReturn("/test-cluster-create");

        Router router = new RoutingDsl()
                .POST("/test-cluster-create").routeTo(() -> {
                    return internalServerError();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("We are currently experiencing difficulties.  Please try again later.");
        carinaClient.createCluster("fake-name", user);

        verify(clusterFactoryMock, times(1)).getCarinaUserUrl(anyString());
        verify(clusterFactoryMock, never()).getCarinaClusterUrl(anyString(), anyString());
    }

    @Test
    public void testCreateClusterStatusError() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaUserUrl(anyString())).thenReturn("/test-cluster-create");

        Router router = new RoutingDsl()
                .POST("/test-cluster-create").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "error");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Cluster ended up in error state");
        carinaClient.createCluster("fake-cluster", user);

        verify(clusterFactoryMock, times(1)).getCarinaUserUrl(anyString());
        verify(clusterFactoryMock, never()).getCarinaClusterUrl(anyString(), anyString());
    }

    @Test
    public void testCreateClusterStatusInvalid() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaUserUrl(anyString())).thenReturn("/test-cluster-create");
        when(clusterFactoryMock.getCarinaClusterUrl(anyString(), anyString())).thenReturn("/test-cluster-get");

        Router router = new RoutingDsl()
                .POST("/test-cluster-create").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "invalid");
                    return ok(Json.toJson(jsonNode));
                })
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "active");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        assertTrue(carinaClient.createCluster("fake-cluster", user));

        verify(clusterFactoryMock, times(1)).getCarinaUserUrl(anyString());
        verify(clusterFactoryMock, times(1)).getCarinaClusterUrl(anyString(), anyString());
    }

    @Test
    public void testGetClusterWithZipSuccess() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.id = 1L;

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaZipUrl(anyString(), anyString())).thenReturn("/test-cluster-get");
        when(carinaFactoryMock.getCarinaDirectoryWithCluster(anyString(), anyString())).thenReturn(Paths.get("/tmp"));
        when(carinaFactoryMock.createFileInCarina(any(), any(), any())).thenReturn(true);

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("zip_url", "/test-cluster-zip");
                    return created(Json.toJson(jsonNode));
                })
                .GET("/test-cluster-zip").routeTo(() -> {
                    return ok(getZipFiles(
                            "test/ca.pem", "test/cert.pem",
                            "test/key.pem", "test/ca-key.pem", "test/docker.env"));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        Cluster returnedCluster = carinaClient.getClusterWithZip(user, "fake-name");
        assertEquals("/tmp", returnedCluster.cert_directory);
        assertEquals("fake-name", returnedCluster.name);
        assertEquals("fake-uri", returnedCluster.uri);

        verify(clusterFactoryMock, times(1)).getCarinaZipUrl(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).getCarinaDirectoryWithCluster(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).createFileInCarina(any(), any(), any());

    }

    @Test
    public void testGetClusterWithZipNoDirectoryZipFile() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.id = 1L;

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaZipUrl(anyString(), anyString())).thenReturn("/test-cluster-get");
        when(carinaFactoryMock.getCarinaDirectoryWithCluster(anyString(), anyString())).thenReturn(Paths.get("/tmp"));
        when(carinaFactoryMock.createFileInCarina(any(), any(), any())).thenReturn(true);

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("zip_url", "/test-cluster-zip");
                    return created(Json.toJson(jsonNode));
                })
                .GET("/test-cluster-zip").routeTo(() -> {
                    return ok(getZipFiles("ca.pem", "cert.pem", "key.pem", "ca-key.pem", "docker.env"));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        Cluster returnedCluster = carinaClient.getClusterWithZip(user, "fake-name");
        assertEquals("/tmp", returnedCluster.cert_directory);
        assertEquals("fake-name", returnedCluster.name);
        assertEquals("fake-uri", returnedCluster.uri);

        verify(clusterFactoryMock, times(1)).getCarinaZipUrl(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).getCarinaDirectoryWithCluster(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).createFileInCarina(any(), any(), any());
    }

    @Test
    public void testGetClusterWithZipUnknownZip() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.id = 1L;

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaZipUrl(anyString(), anyString())).thenReturn("/test-cluster-get");
        when(carinaFactoryMock.getCarinaDirectoryWithCluster(anyString(), anyString())).thenReturn(Paths.get("/tmp"));
        when(carinaFactoryMock.createFileInCarina(any(), any(), any())).thenReturn(true);

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("zip_url", "/test-cluster-zip");
                    return created(Json.toJson(jsonNode));
                })
                .GET("/test-cluster-zip").routeTo(() -> {
                    return ok(getZipFiles("test.pem"));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        Cluster returnedCluster = carinaClient.getClusterWithZip(user, "fake-name");
        assertEquals("/tmp", returnedCluster.cert_directory);
        assertEquals("fake-name", returnedCluster.name);
        assertNull(returnedCluster.uri);

        verify(clusterFactoryMock, times(1)).getCarinaZipUrl(anyString(), anyString());
        verify(carinaFactoryMock, times(1)).getCarinaDirectoryWithCluster(anyString(), anyString());
        verify(carinaFactoryMock, never()).createFileInCarina(any(), any(), any());
    }


    @Test
    public void testGetClusterWithZipUrlNull() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaZipUrl(anyString(), anyString())).thenReturn(null);

        Router router = new RoutingDsl()
                .POST("/test-cluster-create").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "active");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("");
        carinaClient.createCluster("fake-name", user);

        verify(clusterFactoryMock, times(1)).getCarinaZipUrl(anyString(), anyString());
        verify(clusterFactoryMock, never()).getCarinaClusterUrl(anyString(), anyString());

    }

    @Test
    public void testGetClusterWithZipUserNull() throws Exception {
        User user = null;

        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("zip_url", "/test-cluster-zip");
                    return created(Json.toJson(jsonNode));
                })
                .GET("/test-cluster-zip").routeTo(() -> {
                    return ok(getZipFiles(
                            "test/ca.pem", "test/cert.pem",
                            "test/key.pem", "test/ca-key.pem", "test/docker.env"));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Required parameters were not provided.");
        carinaClient.getClusterWithZip(user, "fake-name");
    }

    @Test
    public void testGetClusterWithZipClusterNull() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.id = 1L;

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaZipUrl(anyString(), anyString())).thenReturn("/test-cluster-get");
        when(carinaFactoryMock.getCarinaDirectoryWithCluster(anyString(), anyString())).thenReturn(Paths.get("/tmp"));
        when(carinaFactoryMock.createFileInCarina(any(), any(), any())).thenReturn(true);

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("zip_url", "/test-cluster-zip");
                    return created(Json.toJson(jsonNode));
                })
                .GET("/test-cluster-zip").routeTo(() -> {
                    return ok(getZipFiles(
                            "test/ca.pem", "test/cert.pem",
                            "test/key.pem", "test/ca-key.pem", "test/docker.env"));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Required parameters were not provided.");
        carinaClient.getClusterWithZip(user, null);
    }

    @Test
    public void testGetClusterWithZip401() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.id = 1L;

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaZipUrl(anyString(), anyString())).thenReturn("/test-cluster-get");
        when(carinaFactoryMock.getCarinaDirectoryWithCluster(anyString(), anyString())).thenReturn(Paths.get("/tmp"));
        when(carinaFactoryMock.createFileInCarina(any(), any(), any())).thenReturn(true);

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    return unauthorized();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Could not retrieve cluster zip.");
        carinaClient.getClusterWithZip(user, "fake-name");

        verify(clusterFactoryMock, times(1)).getCarinaZipUrl(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).getCarinaDirectoryWithCluster(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).createFileInCarina(any(), any(), any());

    }

    @Test
    public void testGetClusterWithZip500() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.id = 1L;

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaZipUrl(anyString(), anyString())).thenReturn("/test-cluster-get");
        when(carinaFactoryMock.getCarinaDirectoryWithCluster(anyString(), anyString())).thenReturn(Paths.get("/tmp"));
        when(carinaFactoryMock.createFileInCarina(any(), any(), any())).thenReturn(true);

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    return internalServerError();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Could not retrieve cluster zip.");
        carinaClient.getClusterWithZip(user, "fake-name");

        verify(clusterFactoryMock, times(1)).getCarinaZipUrl(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).getCarinaDirectoryWithCluster(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).createFileInCarina(any(), any(), any());

    }

    @Test
    public void testGetClusterWithZipInnerRequest401() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.id = 1L;

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaZipUrl(anyString(), anyString())).thenReturn("/test-cluster-get");
        when(carinaFactoryMock.getCarinaDirectoryWithCluster(anyString(), anyString())).thenReturn(Paths.get("/tmp"));
        when(carinaFactoryMock.createFileInCarina(any(), any(), any())).thenReturn(true);

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("zip_url", "/test-cluster-zip");
                    return created(Json.toJson(jsonNode));
                })
                .GET("/test-cluster-zip").routeTo(() -> {
                    return unauthorized();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Could not retrieve cluster zip.");
        carinaClient.getClusterWithZip(user, "fake-name");

        verify(clusterFactoryMock, times(1)).getCarinaZipUrl(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).getCarinaDirectoryWithCluster(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).createFileInCarina(any(), any(), any());

    }

    @Test
    public void testGetClusterWithZipInnerRequest500() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.id = 1L;

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaZipUrl(anyString(), anyString())).thenReturn("/test-cluster-get");
        when(carinaFactoryMock.getCarinaDirectoryWithCluster(anyString(), anyString())).thenReturn(Paths.get("/tmp"));
        when(carinaFactoryMock.createFileInCarina(any(), any(), any())).thenReturn(true);

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("zip_url", "/test-cluster-zip");
                    return created(Json.toJson(jsonNode));
                })
                .GET("/test-cluster-zip").routeTo(() -> {
                    return internalServerError();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient  = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Could not retrieve cluster zip.");
        carinaClient.getClusterWithZip(user, "fake-name");

        verify(clusterFactoryMock, times(1)).getCarinaZipUrl(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).getCarinaDirectoryWithCluster(anyString(), anyString());
        verify(carinaFactoryMock, times(5)).createFileInCarina(any(), any(), any());

    }

    @Test
    public void testGetClusterSuccess() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaClusterUrl(anyString(), anyString())).thenReturn("/test-cluster-get");

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "active");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        JsonNode jsonNode = carinaClient.getCluster("fake-name", user);
        assertEquals("active", jsonNode.asText());

        verify(clusterFactoryMock).getCarinaClusterUrl(anyString(), anyString());

    }

    @Test
    public void testGetClusterNameNull() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaClusterUrl(anyString(), anyString())).thenReturn("/test-cluster-get");

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "active");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Required parameters were not provided.");
        carinaClient.getCluster(null, user);
    }

    @Test
    public void testGetClusterClusterNull() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaClusterUrl(anyString(), anyString())).thenReturn("/test-cluster-get");

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("status", "active");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Required parameters were not provided.");
        carinaClient.getCluster("fake-name", null);
    }

    @Test
    public void testGetCluster404() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaClusterUrl(anyString(), anyString())).thenReturn("/test-cluster-get");

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    return notFound();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(NotFoundException.class);
        exception.expectMessage("Cluster not found.");
        carinaClient.getCluster("fake-name", user);

    }

    @Test
    public void testGetCluster500() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaClusterUrl(anyString(), anyString())).thenReturn("/test-cluster-get");

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    return internalServerError();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Didn't expect that!");
        carinaClient.getCluster("fake-name", user);

    }

    @Test
    public void testGetClusterInvalidJson() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IClusterFactory clusterFactoryMock = mock(IClusterFactory.class);
        ICarinaFactory carinaFactoryMock = mock(ICarinaFactory.class);

        when(clusterFactoryMock.getCarinaClusterUrl(anyString(), anyString())).thenReturn("/test-cluster-get");

        Router router = new RoutingDsl()
                .GET("/test-cluster-get").routeTo(() -> {
                    ObjectNode jsonNode = Json.newObject();
                    jsonNode.put("random", "data");
                    return ok(Json.toJson(jsonNode));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        CarinaClient carinaClient = new CarinaClient(clusterFactoryMock, carinaFactoryMock);
        carinaClient.wsClient = ws;

        assertNull(carinaClient.getCluster("fake-name", user));

        verify(clusterFactoryMock).getCarinaClusterUrl(anyString(), anyString());

    }

    private byte[] getZipFiles(String... filenames){
        try(
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for(String filename: filenames){
                ZipEntry entry = new ZipEntry(filename);

                zipOutputStream.putNextEntry(entry);
                if(filename.endsWith("docker.env"))
                    zipOutputStream.write("export DOCKER_HOST=fake-uri".getBytes());
                else
                    zipOutputStream.write("privkeytest".getBytes());
                zipOutputStream.closeEntry();
            }

            return outputStream.toByteArray();

        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        return null;
    }
}