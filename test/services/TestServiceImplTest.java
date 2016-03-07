package services;

import clients.IDockerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import factories.IClusterFactory;
import factories.TestFactory;
import models.Cluster;
import models.TestRequest;
import models.User;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by dimi5963 on 3/7/16.
 */
public class TestServiceImplTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testTestReposeInstanceSuccess() throws InternalServerException{
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        ObjectNode responseNode = JsonNodeFactory.instance.objectNode();
        responseNode.put("message", "success");

        ObjectNode requestNode = JsonNodeFactory.instance.objectNode();
        requestNode.put("message", "success");

        TestRequest testRequest = new TestRequest("GET", "/", null, null);

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);
        TestFactory testFactory = mock(TestFactory.class);
        IClusterService clusterService = mock(IClusterService.class);

        when(testFactory.translateRequest(any())).thenReturn(testRequest);
        when(clusterFactory.getClusterName()).thenReturn("fake-name");
        when(clusterService.getClusterByName(any(),any(),anyBoolean())).thenReturn(cluster);
        when(dockerClient.executeTestAgainstRepose(any(), any(), any(), any())).thenReturn(responseNode);

        JsonNode returnedResult = new TestServiceImpl(clusterService, clusterFactory, dockerClient, testFactory)
                .testReposeInstance(user, "1", requestNode);

        assertEquals(responseNode, returnedResult);

        verify(testFactory).translateRequest(any());
        verify(clusterFactory).getClusterName();
        verify(clusterService).getClusterByName(any(), any(), anyBoolean());
        verify(dockerClient).executeTestAgainstRepose(any(), any(), any(), any());
    }

    @Test
    public void testTestReposeInstanceClusterNameDNE() throws InternalServerException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        ObjectNode responseNode = JsonNodeFactory.instance.objectNode();
        responseNode.put("message", "success");

        ObjectNode requestNode = JsonNodeFactory.instance.objectNode();
        requestNode.put("message", "success");

        TestRequest testRequest = new TestRequest("GET", "/", null, null);

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);
        TestFactory testFactory = mock(TestFactory.class);
        IClusterService clusterService = mock(IClusterService.class);

        when(testFactory.translateRequest(any())).thenReturn(testRequest);
        when(clusterFactory.getClusterName()).thenReturn(null);
        when(clusterService.getClusterByName(any(),any(),anyBoolean())).thenReturn(cluster);
        when(dockerClient.executeTestAgainstRepose(any(), any(), any(), any())).thenReturn(null);

        exception.expect(InternalServerException.class);
        exception.expectMessage("What cluster am I supposed to create?  Misconfigured.");
        new TestServiceImpl(clusterService, clusterFactory, dockerClient, testFactory)
                .testReposeInstance(user, "1", requestNode);

        verify(testFactory).translateRequest(any());
        verify(clusterFactory).getClusterName();
        verify(clusterService).getClusterByName(any(), any(), anyBoolean());
        verify(dockerClient).executeTestAgainstRepose(any(), any(), any(), any());
    }

    @Test
    public void testTestReposeInstanceClusterDNE() throws InternalServerException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        ObjectNode responseNode = JsonNodeFactory.instance.objectNode();
        responseNode.put("message", "success");

        ObjectNode requestNode = JsonNodeFactory.instance.objectNode();
        requestNode.put("message", "success");

        TestRequest testRequest = new TestRequest("GET", "/", null, null);

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);
        TestFactory testFactory = mock(TestFactory.class);
        IClusterService clusterService = mock(IClusterService.class);

        when(testFactory.translateRequest(any())).thenReturn(testRequest);
        when(clusterFactory.getClusterName()).thenReturn("fake-name");
        when(clusterService.getClusterByName(any(),any(),anyBoolean())).thenReturn(null);
        when(dockerClient.executeTestAgainstRepose(any(), any(), any(), any())).thenReturn(null);

        exception.expect(InternalServerException.class);
        exception.expectMessage("No cluster found.  Cluster creation failed and didn't throw an error.");
        new TestServiceImpl(clusterService, clusterFactory, dockerClient, testFactory)
                .testReposeInstance(user, "1", requestNode);

        verify(testFactory).translateRequest(any());
        verify(clusterFactory).getClusterName();
        verify(clusterService).getClusterByName(any(), any(), anyBoolean());
        verify(dockerClient).executeTestAgainstRepose(any(), any(), any(), any());
    }

    @Test
    public void testTestReposeInstanceRequestNull() throws InternalServerException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        ObjectNode responseNode = JsonNodeFactory.instance.objectNode();
        responseNode.put("message", "success");

        TestRequest testRequest = new TestRequest("GET", "/", null, null);

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);
        TestFactory testFactory = mock(TestFactory.class);
        IClusterService clusterService = mock(IClusterService.class);

        when(testFactory.translateRequest(any())).thenReturn(testRequest);
        when(clusterFactory.getClusterName()).thenReturn("fake-name");
        when(clusterService.getClusterByName(any(),any(),anyBoolean())).thenReturn(cluster);
        when(dockerClient.executeTestAgainstRepose(any(), any(), any(), any())).thenReturn(null);

        exception.expect(InternalServerException.class);
        exception.expectMessage("Request is malformed.");
        new TestServiceImpl(clusterService, clusterFactory, dockerClient, testFactory)
                .testReposeInstance(user, "1", null);

        verify(testFactory).translateRequest(any());
        verify(clusterFactory).getClusterName();
        verify(clusterService).getClusterByName(any(), any(), anyBoolean());
        verify(dockerClient).executeTestAgainstRepose(any(), any(), any(), any());
    }

    @Test
    public void testTestReposeInstanceException() throws Exception {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        ObjectNode responseNode = JsonNodeFactory.instance.objectNode();
        responseNode.put("message", "success");

        ObjectNode requestNode = JsonNodeFactory.instance.objectNode();
        requestNode.put("message", "success");

        TestRequest testRequest = new TestRequest("GET", "/", null, null);

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);
        TestFactory testFactory = mock(TestFactory.class);
        IClusterService clusterService = mock(IClusterService.class);

        when(testFactory.translateRequest(any())).thenReturn(testRequest);
        when(clusterFactory.getClusterName()).thenReturn("fake-name");
        when(clusterService.getClusterByName(any(),any(),anyBoolean())).thenReturn(cluster);
        when(dockerClient.executeTestAgainstRepose(any(), any(), any(), any())).
                thenThrow(new InternalServerException("failed to execute."));

        exception.expect(InternalServerException.class);
        exception.expectMessage("failed to execute.");
        new TestServiceImpl(clusterService, clusterFactory, dockerClient, testFactory)
                .testReposeInstance(user, "1", requestNode);

        verify(testFactory).translateRequest(any());
        verify(clusterFactory).getClusterName();
        verify(clusterService).getClusterByName(any(), any(), anyBoolean());
        verify(dockerClient).executeTestAgainstRepose(any(), any(), any(), any());
    }
}