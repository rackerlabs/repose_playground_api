package services;

import clients.IDockerClient;
import exceptions.InternalServerException;
import factories.IClusterFactory;
import models.Cluster;
import models.Configuration;
import models.User;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by dimi5963 on 3/5/16.
 */
public class ConfigurationServiceImplTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    //testGetConfigurationsForInstance

    @Test
    public void testGetConfigurationsForInstanceSuccess() {
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
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock list
        List<Configuration> configurationList = new ArrayList<Configuration>(){
            {
                add(new Configuration("filter-name", "filter-xml"));
                add(new Configuration("filter-name2", "filter-xml2"));
                add(new Configuration("filter-name3", "filter-xml3"));
            }
        };

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean())).thenReturn(cluster);
            when(dockerClient.getConfigurationsForInstance(any(), anyString())).thenReturn(configurationList);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");

        try {
            when(dockerClient.stopReposeInstance(any(), any())).thenReturn(true);
            assertEquals(configurationList, new ConfigurationServiceImpl(clusterService, clusterFactory, dockerClient).
                    getConfigurationsForInstance(user, "1"));
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        verify(clusterFactory).getClusterName();

        try{
            verify(dockerClient).getConfigurationsForInstance(any(), any());
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testGetConfigurationsForInstanceClusterNameNull() throws InternalServerException{
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
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn(null);
        when(dockerClient.getConfigurationsForInstance(any(), any())).thenReturn(null);

        exception.expect(InternalServerException.class);
        exception.expectMessage("What cluster am I supposed to create?  Misconfigured.");
        new ConfigurationServiceImpl(clusterService, clusterFactory, dockerClient).
                getConfigurationsForInstance(user, "1");


        verify(clusterFactory).getClusterName();
        verify(dockerClient).getConfigurationsForInstance(any(), any());

        try{
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetConfigurationsForInstanceClusterNull() throws InternalServerException{

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
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean())).thenReturn(null);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");


        exception.expect(InternalServerException.class);
        exception.expectMessage("No cluster found.  Cluster creation failed and didn't throw an error.");
        new ConfigurationServiceImpl(clusterService, clusterFactory, dockerClient).
                getConfigurationsForInstance(user, "1");

        verify(clusterFactory).getClusterName();

        try{
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testGetConfigurationsForInstanceException() throws InternalServerException{

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
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterService clusterService = mock(IClusterService.class);
        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(clusterService.getClusterByName(anyString(), any(), anyBoolean())).thenReturn(cluster);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        when(clusterFactory.getClusterName()).thenReturn("fake-name");

        when(dockerClient.getConfigurationsForInstance(any(), any())).
                thenThrow(new InternalServerException("Get configs failed."));

        exception.expect(InternalServerException.class);
        exception.expectMessage("Get configs failed.");
        new ConfigurationServiceImpl(clusterService, clusterFactory, dockerClient).
                getConfigurationsForInstance(user, "1");

        verify(clusterFactory).getClusterName();

        try{
            verify(dockerClient).getConfigurationsForInstance(any(), any());
            verify(clusterService).getClusterByName(anyString(), any(), anyBoolean());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }

    }

}