package services;

import clients.IDockerClient;
import exceptions.InternalServerException;
import factories.IClusterFactory;
import models.Cluster;
import models.Container;
import models.User;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by dimi5963 on 3/1/16.
 */
public class ReposeServiceTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testGetReposeList() {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //set up mock container list
        List<Container> containerList = new ArrayList<Container>(){
            {
                add(new Container("fake-name", true, "started 1 sec ago", "1.0", "1"));
                add(new Container("fake-name2", false, "exited 1 sec ago", "1.1", "2"));
            }
        };

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");

        try {
            when(dockerClient.getReposeContainers(any(), any())).thenReturn(containerList);
            List<Container> returnedContainerList =
                    new ReposeService(clusterFactory, clusterService, dockerClient).getReposeList(user);
            assertEquals(returnedContainerList.size(), containerList.size());
            assertArrayEquals(containerList.toArray(), returnedContainerList.toArray());
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        verify(clusterFactory).getClusterName();

        try{
            verify(dockerClient).getReposeContainers(any(), any());
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testGetReposeListClusterNameNull() throws InternalServerException{
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //set up mock container list
        List<Container> containerList = new ArrayList<Container>(){
            {
                add(new Container("fake-name", true, "started 1 sec ago", "1.0", "1"));
                add(new Container("fake-name2", false, "exited 1 sec ago", "1.1", "2"));
            }
        };

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn(null);
        when(dockerClient.getReposeContainers(any(), any())).thenReturn(containerList);

        exception.expect(InternalServerException.class);
        exception.expectMessage("What cluster am I supposed to create?  Misconfigured.");
        new ReposeService(clusterFactory, clusterService, dockerClient).getReposeList(user);


        verify(clusterFactory).getClusterName();
        verify(dockerClient).getReposeContainers(any(), any());

        try{
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetReposeListClusterNull() throws InternalServerException{

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //set up mock container list
        List<Container> containerList = new ArrayList<Container>(){
            {
                add(new Container("fake-name", true, "started 1 sec ago", "1.0", "1"));
                add(new Container("fake-name2", false, "exited 1 sec ago", "1.1", "2"));
            }
        };

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(null);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");
        when(dockerClient.getReposeContainers(any(), any())).thenReturn(containerList);


        exception.expect(InternalServerException.class);
        exception.expectMessage("No cluster found.  Cluster creation failed and didn't throw an error.");
        new ReposeService(clusterFactory, clusterService, dockerClient).getReposeList(user);

        verify(clusterFactory).getClusterName();
        verify(dockerClient).getReposeContainers(any(), any());

        try{
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testGetReposeListEmptyList() {

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");

        try {
            when(dockerClient.getReposeContainers(any(), any())).thenReturn(null);
            new ReposeService(clusterFactory, clusterService, dockerClient).getReposeList(user);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        verify(clusterFactory).getClusterName();

        try{
            verify(dockerClient).getReposeContainers(any(), any());
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    //testStartRepose

    @Test
    public void testStartReposeSuccess() {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");

        try {
            when(dockerClient.startReposeInstance(any(), any())).thenReturn(true);
            assertTrue(new ReposeService(clusterFactory, clusterService, dockerClient).startReposeInstance(user, "1"));
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        verify(clusterFactory).getClusterName();

        try{
            verify(dockerClient).startReposeInstance(any(), any());
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testStartReposeClusterNameNull() throws InternalServerException{
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn(null);
        when(dockerClient.startReposeInstance(any(), any())).thenReturn(true);

        exception.expect(InternalServerException.class);
        exception.expectMessage("What cluster am I supposed to create?  Misconfigured.");
        new ReposeService(clusterFactory, clusterService, dockerClient).startReposeInstance(user, "1");


        verify(clusterFactory).getClusterName();
        verify(dockerClient).startReposeInstance(any(), any());

        try{
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testStartReposeClusterNull() throws InternalServerException{

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(null);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");
        when(dockerClient.startReposeInstance(any(), any())).thenReturn(true);


        exception.expect(InternalServerException.class);
        exception.expectMessage("No cluster found.  Cluster creation failed and didn't throw an error.");
        new ReposeService(clusterFactory, clusterService, dockerClient).startReposeInstance(user, "1");

        verify(clusterFactory).getClusterName();
        verify(dockerClient).startReposeInstance(any(), any());

        try{
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testStartReposeException() throws InternalServerException{

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");

        when(dockerClient.startReposeInstance(any(), any())).thenThrow(new InternalServerException("Start repose instance"));

        exception.expect(InternalServerException.class);
        exception.expectMessage("Start repose instance");
        new ReposeService(clusterFactory, clusterService, dockerClient).startReposeInstance(user, "1");

        verify(clusterFactory).getClusterName();

        try{
            verify(dockerClient).startReposeInstance(any(), any());
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    //testStopRepose

    @Test
    public void testStopReposeSuccess() {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");

        try {
            when(dockerClient.stopReposeInstance(any(), any())).thenReturn(true);
            assertTrue(new ReposeService(clusterFactory, clusterService, dockerClient).stopReposeInstance(user, "1"));
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        verify(clusterFactory).getClusterName();

        try{
            verify(dockerClient).stopReposeInstance(any(), any());
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testStopReposeClusterNameNull() throws InternalServerException{
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn(null);
        when(dockerClient.stopReposeInstance(any(), any())).thenReturn(true);

        exception.expect(InternalServerException.class);
        exception.expectMessage("What cluster am I supposed to create?  Misconfigured.");
        new ReposeService(clusterFactory, clusterService, dockerClient).stopReposeInstance(user, "1");


        verify(clusterFactory).getClusterName();
        verify(dockerClient).stopReposeInstance(any(), any());

        try{
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testStopReposeClusterNull() throws InternalServerException{

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(null);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");
        when(dockerClient.stopReposeInstance(any(), any())).thenReturn(true);


        exception.expect(InternalServerException.class);
        exception.expectMessage("No cluster found.  Cluster creation failed and didn't throw an error.");
        new ReposeService(clusterFactory, clusterService, dockerClient).stopReposeInstance(user, "1");

        verify(clusterFactory).getClusterName();
        verify(dockerClient).stopReposeInstance(any(), any());

        try{
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testStopReposeException() throws InternalServerException{

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");

        when(dockerClient.stopReposeInstance(any(), any())).thenThrow(new InternalServerException("Stop repose instance"));

        exception.expect(InternalServerException.class);
        exception.expectMessage("Stop repose instance");
        new ReposeService(clusterFactory, clusterService, dockerClient).stopReposeInstance(user, "1");

        verify(clusterFactory).getClusterName();

        try{
            verify(dockerClient).stopReposeInstance(any(), any());
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

}