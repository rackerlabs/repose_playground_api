package clients;

import exceptions.InternalServerException;
import factories.ComponentFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import play.api.routing.Router;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

/**
 * Created by dimi5963 on 3/8/16.
 */
public class ComponentClientImplTest {

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
    public void testGetFiltersByVersionSuccess() throws Exception {
        ComponentFactory componentFactory = mock(ComponentFactory.class);

        when(componentFactory.getAvailableFilters()).thenReturn(null);
        when(componentFactory.getFilterPomUrl(anyString())).thenReturn("/filters-by-version");

        Router router = new RoutingDsl()
                .GET("/filters-by-version").routeTo(() -> {
                    return ok("<test>" +
                            "<dependency><artifactId>add-header</artifactId></dependency>" +
                            "<dependency><artifactId>ip-user</artifactId></dependency>" +
                            "<dependency><artifactId>compression</artifactId></dependency>" +
                            "</test>");
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        ComponentClientImpl componentClient = new ComponentClientImpl(componentFactory);
        componentClient.wsClient = ws;

        try {
            List<String> filters = componentClient.getFiltersByVersion("1");
            assertEquals(3, filters.size());
            assertTrue(filters.stream().anyMatch(t -> t.equals("add-header")));
            assertTrue(filters.stream().anyMatch(t -> t.equals("compression")));
        }catch(InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        verify(componentFactory, times(1)).getAvailableFilters();
        verify(componentFactory, times(1)).getFilterPomUrl(anyString());
    }

    @Test
    public void testGetFiltersByVersionWithCustomFilter() throws Exception {
        ComponentFactory componentFactory = mock(ComponentFactory.class);

        when(componentFactory.getAvailableFilters()).thenReturn(
                new ArrayList<String>(){
                    {
                        add("add-header");
                        add("ip-user");
                    }
                }
        );
        when(componentFactory.getFilterPomUrl(anyString())).thenReturn("/filters-by-version");

        Router router = new RoutingDsl()
                .GET("/filters-by-version").routeTo(() -> {
                    return ok("<test>" +
                            "<dependency><artifactId>add-header</artifactId></dependency>" +
                            "<dependency><artifactId>ip-user</artifactId></dependency>" +
                            "<dependency><artifactId>compression</artifactId></dependency>" +
                            "</test>");
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        ComponentClientImpl componentClient = new ComponentClientImpl(componentFactory);
        componentClient.wsClient = ws;

        try {
            List<String> filters = componentClient.getFiltersByVersion("1");
            assertEquals(2, filters.size());
            assertTrue(filters.stream().anyMatch(t -> t.equals("add-header")));
            assertFalse(filters.stream().anyMatch(t -> t.equals("compression")));
        }catch(InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

        verify(componentFactory, times(1)).getAvailableFilters();
        verify(componentFactory, times(1)).getFilterPomUrl(anyString());
    }

    @Test
    public void testGetFiltersByVersionException() throws Exception {
        ComponentFactory componentFactory = mock(ComponentFactory.class);

        when(componentFactory.getAvailableFilters()).thenReturn(null);
        when(componentFactory.getFilterPomUrl(anyString())).thenReturn("/filters-by-version");

        Router router = new RoutingDsl()
                .GET("/filters-by-version").routeTo(() -> {
                    return ok();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        ComponentClientImpl componentClient = new ComponentClientImpl(componentFactory);
        componentClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("We are currently experiencing difficulties.  Please try again later.");
        componentClient.getFiltersByVersion("1");

        verify(componentFactory, times(1)).getAvailableFilters();
        verify(componentFactory, times(1)).getFilterPomUrl(anyString());
    }

    @Test
    public void testGetFiltersByVersion404() throws Exception {
        ComponentFactory componentFactory = mock(ComponentFactory.class);

        when(componentFactory.getAvailableFilters()).thenReturn(null);
        when(componentFactory.getFilterPomUrl(anyString())).thenReturn("/filters-by-version");

        Router router = new RoutingDsl()
                .GET("/filters-by-version").routeTo(() -> {
                    return notFound();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        ComponentClientImpl componentClient = new ComponentClientImpl(componentFactory);
        componentClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("We are currently experiencing difficulties.  Please try again later.");
        componentClient.getFiltersByVersion("1");

        verify(componentFactory, times(1)).getAvailableFilters();
        verify(componentFactory, times(1)).getFilterPomUrl(anyString());
    }

    @Test
    public void testGetComponentXSDSuccess() throws Exception {
        ComponentFactory componentFactory = mock(ComponentFactory.class);

        when(componentFactory.getBindingsUrl(anyString(), anyString())).thenReturn("/bindings");
        when(componentFactory.getSchemaUrl(anyString(), anyString(), anyString())).thenReturn("/schema-location");

        Router router = new RoutingDsl()
                .GET("/bindings").routeTo(() -> {
                    return ok("<bindings schemaLocation=\"test\"/>");
                })
                .GET("/schema-location").routeTo(() -> {
                    return ok("<test>" +
                            "<component>data</component>" +
                            "</test>");
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        ComponentClientImpl componentClient = new ComponentClientImpl(componentFactory);
        componentClient.wsClient = ws;

        Document returnDoc = componentClient.getComponentXSD("1", "2");
        assertEquals(1, returnDoc.getElementsByTagName("component").getLength());
        assertEquals("data", returnDoc.getElementsByTagName("component").item(0).getTextContent());

        verify(componentFactory, times(1)).getBindingsUrl(anyString(), anyString());
        verify(componentFactory, times(1)).getSchemaUrl(anyString(), anyString(), anyString());
    }

    @Test
    public void testGetComponentXSDNotXmlResponse() throws Exception {
        ComponentFactory componentFactory = mock(ComponentFactory.class);

        when(componentFactory.getBindingsUrl(anyString(), anyString())).thenReturn("/bindings");
        when(componentFactory.getSchemaUrl(anyString(), anyString(), anyString())).thenReturn("/schema-location");

        Router router = new RoutingDsl()
                .GET("/bindings").routeTo(() -> {
                    return ok("<bindings schemaLocation=\"test\"/>");
                })
                .GET("/schema-location").routeTo(() -> {
                    return ok("{\"message\":\"test\"}");
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        ComponentClientImpl componentClient = new ComponentClientImpl(componentFactory);
        componentClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("We are currently experiencing difficulties.  Please try again later.");
        componentClient.getComponentXSD("1", "2");

        verify(componentFactory, times(1)).getBindingsUrl(anyString(), anyString());
        verify(componentFactory, times(1)).getSchemaUrl(anyString(), anyString(), anyString());

    }

    @Test
    public void testGetComponentXSD404() throws Exception {
        ComponentFactory componentFactory = mock(ComponentFactory.class);

        when(componentFactory.getBindingsUrl(anyString(), anyString())).thenReturn("/bindings");

        Router router = new RoutingDsl()
                .GET("/bindings").routeTo(() -> {
                    return notFound();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        ComponentClientImpl componentClient = new ComponentClientImpl(componentFactory);
        componentClient.wsClient = ws;

        assertNull(componentClient.getComponentXSD("1", "2"));

        verify(componentFactory, times(1)).getBindingsUrl(anyString(), anyString());
        verify(componentFactory, never()).getSchemaUrl(anyString(), anyString(), anyString());

    }

    @Test
    public void testGetComponentXSDSchemaLocation404() throws Exception {
        ComponentFactory componentFactory = mock(ComponentFactory.class);

        when(componentFactory.getBindingsUrl(anyString(), anyString())).thenReturn("/bindings");
        when(componentFactory.getSchemaUrl(anyString(), anyString(), anyString())).thenReturn("/schema-location");

        Router router = new RoutingDsl()
                .GET("/bindings").routeTo(() -> {
                    return ok("<bindings schemaLocation=\"test\"/>");
                })
                .GET("/schema-location").routeTo(() -> {
                    return notFound();
                })
                .build();

        server = Server.forRouter(router);
        ws = WS.newClient(server.httpPort());
        ComponentClientImpl componentClient = new ComponentClientImpl(componentFactory);
        componentClient.wsClient = ws;

        exception.expect(InternalServerException.class);
        exception.expectMessage("We are currently experiencing difficulties.  Please try again later.");
        componentClient.getComponentXSD("1", "2");

        verify(componentFactory, times(1)).getBindingsUrl(anyString(), anyString());
        verify(componentFactory, times(1)).getSchemaUrl(anyString(), anyString(), anyString());

    }
}