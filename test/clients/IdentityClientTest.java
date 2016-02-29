package clients;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import exceptions.UnauthorizedException;
import factories.IIdentityFactory;
import models.AuthRequest;
import models.LoginRequest;
import models.PasswordCredsRequest;
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
import repositories.IUserRepository;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.*;
import static play.mvc.Results.*;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class IdentityClientTest {

    private WSClient ws;
    private Server server;

    private IdentityClient identityClient;

    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @After
    public void tearDown() {
        ws.close();
        server.stop();
    }

    @Test
    public void testGetUserSuccess() {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityAuthUrl()).thenReturn("/test-user-create");
        when(userRepositoryMock.saveUser(any(), any(), any())).thenReturn(user);

        Router router = new RoutingDsl()
                .POST("/test-user-create").routeTo(() -> {
                    return ok(Json.toJson(user));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        User returnedUser = null;
        try {
            returnedUser = identityClient.getUser(new LoginRequest(
                    new AuthRequest(
                            new PasswordCredsRequest("fake-user", "fake-pass"))));
        }catch(UnauthorizedException | InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        assertEquals(returnedUser.userid, user.userid);
        assertEquals(returnedUser.username, user.username);
        assertEquals(returnedUser.tenant, user.tenant);

        verify(identityFactoryMock).getIdentityAuthUrl();
        verify(userRepositoryMock).saveUser(any(), any(), any());
    }

    @Test
    public void testGetUserNoPassword() {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityAuthUrl()).thenReturn("/test-user-create");
        when(userRepositoryMock.saveUser(any(), any(), any())).thenReturn(user);

        Router router = new RoutingDsl()
                .POST("/test-user-create").routeTo(() -> {
                    return ok(Json.toJson(user));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;


        User returnedUser = null;
        try {
            returnedUser = identityClient.getUser(new LoginRequest(
                    new AuthRequest(
                            new PasswordCredsRequest("fake-user", null))));
        }catch(UnauthorizedException | InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        assertEquals(returnedUser.userid, user.userid);
        assertEquals(returnedUser.username, user.username);
        assertEquals(returnedUser.tenant, user.tenant);

        verify(identityFactoryMock).getIdentityAuthUrl();
        verify(userRepositoryMock, times(1)).saveUser(any(), any(), any());
    }

    @Test
    public void testGetUserNoUsername() {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityAuthUrl()).thenReturn("/test-user-create");
        when(userRepositoryMock.saveUser(any(), any(), any())).thenReturn(user);

        Router router = new RoutingDsl()
                .POST("/test-user-create").routeTo(() -> {
                    return ok(Json.toJson(user));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        User returnedUser = null;
        try {
            returnedUser = identityClient.getUser(new LoginRequest(
                    new AuthRequest(
                            new PasswordCredsRequest(null, "fake-pass"))));
        }catch(UnauthorizedException | InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        assertEquals(returnedUser.userid, user.userid);
        assertEquals(returnedUser.username, user.username);
        assertEquals(returnedUser.tenant, user.tenant);

        verify(identityFactoryMock).getIdentityAuthUrl();
        verify(userRepositoryMock, times(1)).saveUser(any(), any(), any());
    }

    @Test
    public void testGetUserUnauthenticated() throws UnauthorizedException, InternalServerException{
        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityAuthUrl()).thenReturn("/test-user-create");

        Router router = new RoutingDsl()
                .POST("/test-user-create").routeTo(() -> {
                    return unauthorized();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        exception.expect(UnauthorizedException.class);
        exception.expectMessage("Unable to authenticate user.");
        identityClient.getUser(new LoginRequest(
                new AuthRequest(
                        new PasswordCredsRequest("fake-user", "fake-pass"))));

        verify(identityFactoryMock).getIdentityAuthUrl();
        verify(userRepositoryMock, never()).saveUser(any(), any(), any());
    }

    @Test
    public void testGetUserInternalServerError() throws UnauthorizedException, InternalServerException {
        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityAuthUrl()).thenReturn("/test-user-create");

        Router router = new RoutingDsl()
                .POST("/test-user-create").routeTo(() -> {
                    return internalServerError();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Unable to authenticate user.");
        identityClient.getUser(new LoginRequest(
                new AuthRequest(
                        new PasswordCredsRequest("fake-user", "fake-pass"))));

        verify(identityFactoryMock).getIdentityAuthUrl();
        verify(userRepositoryMock, never()).saveUser(any(), any(), any());
    }

    @Test
    public void testGetUserRandomFailure() throws UnauthorizedException, InternalServerException{
        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        //fake a non existing endpoint.  Identity is down
        when(identityFactoryMock.getIdentityAuthUrl()).thenReturn("/THIS-IS-ALL-WRONG");

        Router router = new RoutingDsl()
                .POST("/test-user-create").routeTo(() -> {
                    return ok();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Unable to authenticate user.");
        identityClient.getUser(new LoginRequest(
                new AuthRequest(
                        new PasswordCredsRequest("fake-user", "fake-pass"))));

        verify(identityFactoryMock).getIdentityAuthUrl();
        verify(userRepositoryMock, never()).saveUser(any(), any(), any());
    }

    @Test
    public void testGetUserNoUserData() throws UnauthorizedException, InternalServerException{
        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityAuthUrl()).thenReturn("/test-user-create");

        Router router = new RoutingDsl()
                .POST("/test-user-create").routeTo(() -> {
                    return ok();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Unable to authenticate user.");
        identityClient.getUser(new LoginRequest(
                new AuthRequest(
                        new PasswordCredsRequest("fake-user", "fake-pass"))));

        verify(identityFactoryMock).getIdentityAuthUrl();
        verify(userRepositoryMock, never()).saveUser(any(), any(), any());
    }


    @Test
    public void testGetUserApiKeySuccess() {
        String apiKey = "fake-key";

        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityApiAuthUrl(anyString())).thenReturn("/test-api-create");

        Router router = new RoutingDsl()
                .GET("/test-api-create").routeTo(() -> {
                    ObjectNode response = JsonNodeFactory.instance.objectNode();
                    ObjectNode apiKeyNode = response.objectNode();
                    apiKeyNode.put("apiKey", "fake-key");
                    response.putPOJO("RAX-KSKEY:apiKeyCredentials", apiKeyNode);
                    return ok(Json.toJson(response));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        try {
            assertEquals(identityClient.getUserApiKey("fake-token", "fake-user"), apiKey);
        }catch(UnauthorizedException | InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        verify(identityFactoryMock).getIdentityAuthUrl();
    }

    @Test
    public void testGetUserApiKeyNoToken() {
        String apiKey = "fake-key";

        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityApiAuthUrl(anyString())).thenReturn("/test-api-create");

        Router router = new RoutingDsl()
                .GET("/test-api-create").routeTo(() -> {
                    ObjectNode response = JsonNodeFactory.instance.objectNode();
                    ObjectNode apiKeyNode = response.objectNode();
                    apiKeyNode.put("apiKey", "fake-key");
                    response.putPOJO("RAX-KSKEY:apiKeyCredentials", apiKeyNode);
                    return ok(Json.toJson(response));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        try {
            assertEquals(identityClient.getUserApiKey(null, "fake-user"), apiKey);
        }catch(UnauthorizedException | InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        verify(identityFactoryMock).getIdentityAuthUrl();
    }

    @Test
    public void testGetUserApiKeyNoTenantUserId() {
        String apiKey = "fake-key";

        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityApiAuthUrl(anyString())).thenReturn("/test-api-create");

        Router router = new RoutingDsl()
                .GET("/test-api-create").routeTo(() -> {
                    ObjectNode response = JsonNodeFactory.instance.objectNode();
                    ObjectNode apiKeyNode = response.objectNode();
                    apiKeyNode.put("apiKey", "fake-key");
                    response.putPOJO("RAX-KSKEY:apiKeyCredentials", apiKeyNode);
                    return ok(Json.toJson(response));
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        try {
            assertEquals(identityClient.getUserApiKey("fake-token", null), apiKey);
        }catch(UnauthorizedException | InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        verify(identityFactoryMock).getIdentityAuthUrl();
    }

    @Test
    public void testGetUserApiKeyUnauthenticated() throws UnauthorizedException, InternalServerException{
        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityApiAuthUrl(anyString())).thenReturn("/test-api-create");

        Router router = new RoutingDsl()
                .GET("/test-api-create").routeTo(() -> {
                    return unauthorized();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        exception.expect(UnauthorizedException.class);
        exception.expectMessage("Unable to retrive api key for user.");
        identityClient.getUserApiKey("fake-token", null);

        verify(identityFactoryMock).getIdentityAuthUrl();
    }

    @Test
    public void testGetUserApiKeyInternalServerError() throws UnauthorizedException, InternalServerException{
        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityApiAuthUrl(anyString())).thenReturn("/test-api-create");

        Router router = new RoutingDsl()
                .GET("/test-api-create").routeTo(() -> {
                    return internalServerError();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Unable to retrive api key for user.");
        identityClient.getUserApiKey("fake-token", null);

        verify(identityFactoryMock).getIdentityAuthUrl();
    }

    @Test
    public void testGetUserApiKeyRandomFailure()  throws UnauthorizedException, InternalServerException{
        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityApiAuthUrl(anyString())).thenReturn("/test-api-create");

        Router router = new RoutingDsl()
                .GET("/test-api-create").routeTo(() -> {
                    return internalServerError();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Unable to retrive api key for user.");
        identityClient.getUserApiKey("fake-token", null);

        verify(identityFactoryMock).getIdentityAuthUrl();
    }

    @Test
    public void testGetUserApiKeyNoUserData() throws UnauthorizedException, InternalServerException{
        IUserRepository userRepositoryMock = mock(IUserRepository.class);
        IIdentityFactory identityFactoryMock = mock(IIdentityFactory.class);

        when(identityFactoryMock.getIdentityApiAuthUrl(anyString())).thenReturn("/test-api-create");

        Router router = new RoutingDsl()
                .GET("/test-api-create").routeTo(() -> {
                    return internalServerError();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        identityClient = new IdentityClient(identityFactoryMock, userRepositoryMock);
        identityClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("Unable to retrive api key for user.");
        identityClient.getUserApiKey("fake-token", null);

        verify(identityFactoryMock).getIdentityAuthUrl();
    }
}
